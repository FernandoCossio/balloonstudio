package com.decoraciones.features.reserva;

import com.decoraciones.common.errors.*;
import com.decoraciones.domain.dtos.reserva.ReservaRequest;
import com.decoraciones.domain.dtos.reserva.ReservaResponse;
import com.decoraciones.domain.models.*;
import com.decoraciones.features.configuracion.ConfiguracionService;
import com.decoraciones.features.elementolienzo.ElementoLienzoRepository;
import com.decoraciones.features.inventario.BloqueoInventarioRepository;
import com.decoraciones.features.pago.PagoRepository;
import com.decoraciones.features.pago.MetodoPagoRepository;
import com.decoraciones.features.pago.StripeService;
import com.decoraciones.features.pago.PagoFacilService;
import com.decoraciones.features.proyectodiseno.ProyectoDisenoRepository;
import com.decoraciones.features.usuario.UsuarioRepository;
import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservaService {

    private final ProyectoDisenoRepository proyectoRepository;
    private final ElementoLienzoRepository elementoRepository;
    private final CotizacionRepository cotizacionRepository;
    private final ReservaRepository reservaRepository;
    private final BloqueoInventarioRepository bloqueoRepository;
    private final PagoRepository pagoRepository;
    private final MetodoPagoRepository metodoPagoRepository;
    private final InventarioLockService lockService;
    private final StripeService stripeService;
    private final UsuarioRepository usuarioRepository;
    private final CotizacionService cotizacionService;
    private final PagoFacilService pagoFacilService;
    private final ConfiguracionService configuracionService;

    @org.springframework.beans.factory.annotation.Value("${app.reserva.lock-ttl-minutes:15}")
    private int lockTtlMinutes;

    @org.springframework.beans.factory.annotation.Value("${pagofacil.test-mode.enabled:false}")
    private boolean testModeEnabled;

    @org.springframework.beans.factory.annotation.Value("${pagofacil.test-mode.monto-simulado:0.10}")
    private double testModeMontoSimulado;
    @Transactional
    public Map<String, Object> iniciarReserva(Long proyectoId, ReservaRequest request) {
        ProyectoDiseno proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(ProyectoDisenoNoEncontradoException::new);

        Usuario usuario = usuarioRepository.findById(request.usuarioId())
                .orElseThrow(UsuarioNoEncontradoException::new);

        LocalDate fechaEvento = proyecto.getFechaEvento() != null ? proyecto.getFechaEvento() : LocalDate.now().plusDays(30);

        // 1. Calcular costos utilizando el motor de cotización real
        List<ElementoLienzo> elementos = elementoRepository.findAllByProyectoIdOrderByZIndexAsc(proyectoId);
        List<com.decoraciones.domain.dtos.proyectodiseno.ElementoLienzoRequest> requests = elementos.stream().map(el -> new com.decoraciones.domain.dtos.proyectodiseno.ElementoLienzoRequest(
                el.getArticuloInventario().getId(),
                el.getCantidad(),
                el.getPosX(), el.getPosY(),
                el.getWidth(), el.getHeight(),
                el.getScaleX(), el.getScaleY(),
                el.getRotacionDeg(), el.getOpacity(),
                el.getZIndex(), el.getLayer(),
                el.getVistaActual()
        )).toList();

        com.decoraciones.domain.dtos.cotizacion.CotizacionDetalleResponse detail = cotizacionService.calcularCotizacion(proyectoId, requests, null);
        BigDecimal costoArticulos = detail.costoArticulos();
        BigDecimal costoFlete = detail.costoFlete();
        BigDecimal costoArmado = detail.costoArmado();
        BigDecimal total = detail.total();

        // 2. Bloqueo temporal en Redis (Estricto - falla si Redis está caído)
        lockService.liberarBloqueosTemporales(proyectoId, true);

        Map<Long, Integer> cantidadesPorArticulo = new HashMap<>();
        Map<Long, ArticuloInventario> articulos = new HashMap<>();
        for (ElementoLienzo el : elementos) {
            Long artId = el.getArticuloInventario().getId();
            cantidadesPorArticulo.put(artId, cantidadesPorArticulo.getOrDefault(artId, 0) + el.getCantidad());
            articulos.put(artId, el.getArticuloInventario());
        }

        for (Map.Entry<Long, Integer> entry : cantidadesPorArticulo.entrySet()) {
            Long artId = entry.getKey();
            Integer cantidad = entry.getValue();
            boolean lockAdquirido = lockService.lockTemporalmente(
                    artId,
                    cantidad,
                    fechaEvento,
                    fechaEvento.plusDays(1),
                    proyectoId,
                    false // strict = false
            );
            if (!lockAdquirido) {
                // Si falla un bloqueo, liberamos los bloqueos de este proyecto en Redis
                lockService.liberarBloqueosTemporales(proyectoId, false);
                throw new StockInsuficienteException("Stock insuficiente para realizar la reserva temporal del artículo: " + articulos.get(artId).getNombre());
            }
        }

        BigDecimal porcentajeAnticipo = BigDecimal.valueOf(0.20); // 20%
        BigDecimal montoAnticipo = total.multiply(porcentajeAnticipo);

        // 3. Crear PaymentIntent en Stripe con metadatos
        Map<String, String> stripeMetadata = Map.of(
                "proyectoId", String.valueOf(proyectoId),
                "usuarioId", String.valueOf(usuario.getId())
        );
        String clientSecret = stripeService.crearPaymentIntent(montoAnticipo, "BOB", stripeMetadata);

        Map<String, Object> data = new HashMap<>();
        data.put("reservaId", proyectoId); // Usamos proyectoId como id temporal para el frontend
        data.put("montoAnticipo", montoAnticipo);
        data.put("stripeClientSecret", clientSecret);
        data.put("expiraEnMinutos", lockTtlMinutes);
        data.put("totalOriginal", total);
        data.put("costoArticulos", costoArticulos);
        data.put("costoFlete", costoFlete);
        data.put("costoArmado", costoArmado);

        return data;
    }

    @Transactional
    public void confirmarPago(Long proyectoId, String referenciaPago) {
        confirmarPago(proyectoId, referenciaPago, "STRIPE");
    }

    @Transactional
    public void confirmarPago(Long proyectoId, String referenciaPago, String metodoPagoNombre) {
        if (pagoRepository.existsByReferenciaExterna(referenciaPago)) {
            log.info("El pago con referencia {} ya está registrado.", referenciaPago);
            return;
        }

        ProyectoDiseno proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(ProyectoDisenoNoEncontradoException::new);
        Usuario usuario = proyecto.getUsuario();

        // 1. Calcular costos utilizando el motor de cotización real
        List<ElementoLienzo> elementos = elementoRepository.findAllByProyectoIdOrderByZIndexAsc(proyectoId);
        List<com.decoraciones.domain.dtos.proyectodiseno.ElementoLienzoRequest> requests = elementos.stream().map(el -> new com.decoraciones.domain.dtos.proyectodiseno.ElementoLienzoRequest(
                el.getArticuloInventario().getId(),
                el.getCantidad(),
                el.getPosX(), el.getPosY(),
                el.getWidth(), el.getHeight(),
                el.getScaleX(), el.getScaleY(),
                el.getRotacionDeg(), el.getOpacity(),
                el.getZIndex(), el.getLayer(),
                el.getVistaActual()
        )).toList();

        com.decoraciones.domain.dtos.cotizacion.CotizacionDetalleResponse detail = cotizacionService.calcularCotizacion(proyectoId, requests, null);
        BigDecimal costoArticulos = detail.costoArticulos();
        BigDecimal costoFlete = detail.costoFlete();
        BigDecimal costoArmado = detail.costoArmado();
        BigDecimal total = detail.total();

        // 2. Crear Cotizacion
        Cotizacion cotizacion = new Cotizacion();
        cotizacion.setProyectoDiseno(proyecto);
        cotizacion.setCostoArticulos(costoArticulos);
        cotizacion.setCostoFlete(costoFlete);
        cotizacion.setCostoArmado(costoArmado);
        cotizacion.setTotal(total);
        cotizacion.setEstado("ACEPTADA");
        cotizacion.setFechaGeneracion(LocalDateTime.now());
        cotizacion.setTasaOverheadAplicada(detail.tasaOverheadAplicada());
        cotizacion = cotizacionRepository.save(cotizacion);

        // 3. Crear Reserva CONFIRMADA
        BigDecimal porcentajeAnticipo = BigDecimal.valueOf(0.20);
        BigDecimal montoAnticipo = total.multiply(porcentajeAnticipo);

        Reserva reserva = new Reserva();
        reserva.setCotizacion(cotizacion);
        reserva.setUsuario(usuario);
        reserva.setEstado("CONFIRMADA");
        reserva.setFechaReserva(LocalDateTime.now());
        reserva.setFechaLimitePago(LocalDateTime.now().plusMinutes(lockTtlMinutes));
        reserva.setFechaConfirmacion(LocalDateTime.now());
        reserva.setMontoAnticipo(montoAnticipo);
        reserva = reservaRepository.save(reserva);

        // 4. Registrar el pago
        Pago pago = new Pago();
        pago.setReserva(reserva);
        pago.setMonto(montoAnticipo);
        pago.setEstado("COMPLETADO");
        pago.setFechaPago(LocalDateTime.now());
        pago.setReferenciaExterna(referenciaPago);
        pago.setTipoPago("ANTICIPO");

        MetodoPago metodo = metodoPagoRepository.findByNombre(metodoPagoNombre.toUpperCase())
                .orElseGet(() -> {
                    MetodoPago mp = new MetodoPago();
                    mp.setNombre(metodoPagoNombre.toUpperCase());
                    mp.setDescripcion("Método auto-creado");
                    return metodoPagoRepository.save(mp);
                });
        pago.setMetodoPago(metodo);
        pagoRepository.save(pago);

        // 5. Trasladar bloqueos temporales de Redis a PostgreSQL permanentemente
        LocalDate fechaEvento = proyecto.getFechaEvento() != null ? proyecto.getFechaEvento() : LocalDate.now().plusDays(30);
        for (ElementoLienzo el : elementos) {
            BloqueoInventario b = new BloqueoInventario();
            b.setArticuloInventario(el.getArticuloInventario());
            b.setReserva(reserva);
            b.setCantidad(el.getCantidad());
            b.setFechaInicio(fechaEvento);
            b.setFechaFin(fechaEvento.plusDays(1));
            b.setTipoBloqueo("CONFIRMADO");
            bloqueoRepository.save(b);
        }

        // 6. Liberar llaves temporales de Redis
        lockService.liberarBloqueosTemporales(proyectoId);

        log.info("Pago confirmado exitosamente para el proyecto ID: {} con el método {}. Reserva confirmada creada.", proyectoId, metodoPagoNombre);
    }

    @Transactional
    public PagoFacilService.PagoFacilQrResponse generarQrPago(
            Long proyectoId,
            String clientName,
            String clientCi,
            String email,
            String phone
    ) {
        ProyectoDiseno proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(ProyectoDisenoNoEncontradoException::new);
        Usuario usuario = proyecto.getUsuario();

        List<ElementoLienzo> elementos = elementoRepository.findAllByProyectoIdOrderByZIndexAsc(proyectoId);
        List<com.decoraciones.domain.dtos.proyectodiseno.ElementoLienzoRequest> requests = elementos.stream().map(el -> new com.decoraciones.domain.dtos.proyectodiseno.ElementoLienzoRequest(
                el.getArticuloInventario().getId(),
                el.getCantidad(),
                el.getPosX(), el.getPosY(),
                el.getWidth(), el.getHeight(),
                el.getScaleX(), el.getScaleY(),
                el.getRotacionDeg(), el.getOpacity(),
                el.getZIndex(), el.getLayer(),
                el.getVistaActual()
        )).toList();

        com.decoraciones.domain.dtos.cotizacion.CotizacionDetalleResponse detail = cotizacionService.calcularCotizacion(proyectoId, requests, null);
        BigDecimal total = detail.total();
        double amount = total.multiply(BigDecimal.valueOf(0.20)).doubleValue();

        if (testModeEnabled) {
            log.info("[PagoFacil TEST MODE] Forzando monto del QR a: {} (Monto original: {})", testModeMontoSimulado, amount);
            amount = testModeMontoSimulado;
        }

        String safeName = (clientName != null && !clientName.trim().isEmpty()) ? clientName.trim() : usuario.getNombreCompleto();
        if (safeName == null || safeName.trim().isEmpty()) {
            safeName = "Cliente";
        }

        String rawEmail = (email != null && !email.trim().isEmpty()) ? email.trim() : usuario.getEmail();
        String safeEmail = "";
        if (rawEmail != null && !rawEmail.trim().isEmpty() && rawEmail.contains("@")) {
            safeEmail = rawEmail.trim();
        }

        String rawPhone = (phone != null && !phone.trim().isEmpty()) ? phone.trim() : usuario.getTelefono();
        String safePhone = "75540850"; // default fallback phone
        if (rawPhone != null && !rawPhone.trim().isEmpty()) {
            String digits = rawPhone.replaceAll("[^0-9]", "");
            if (digits.length() >= 7) {
                safePhone = digits;
            }
        }

        String safeCi = (clientCi != null && !clientCi.trim().isEmpty()) ? clientCi.trim() : "1234567";

        String token = pagoFacilService.login();
        return pagoFacilService.generarQR(
                token,
                proyectoId + "-" + System.currentTimeMillis(),
                amount,
                safeName,
                safeCi,
                safeEmail,
                safePhone
        );
    }

    @Transactional
    public boolean verificarEstadoPagoQr(Long proyectoId, String transactionId) {
        String token = pagoFacilService.login();
        boolean pagado = pagoFacilService.consultarTransaccion(token, transactionId);

        if (pagado) {
            log.info("PagoFacil confirmó el pago para proyecto ID: {}, transactionId: {}. Confirmando reserva...", proyectoId, transactionId);
            confirmarPago(proyectoId, transactionId, "PAGO_FACIL");
        }

        return pagado;
    }

    private ReservaResponse mapToResponse(Reserva r) {
        Cotizacion cot = r.getCotizacion();
        ProyectoDiseno proj = cot != null ? cot.getProyectoDiseno() : null;
        Usuario usr = r.getUsuario();

        return new ReservaResponse(
                r.getId(),
                usr != null ? usr.getId() : null,
                usr != null ? usr.getNombreCompleto() : null,
                usr != null ? usr.getEmail() : null,
                usr != null ? usr.getTelefono() : null,
                proj != null ? proj.getId() : null,
                proj != null ? proj.getNombre() : null,
                proj != null ? proj.getFechaEvento() : null,
                proj != null ? proj.getLugarEvento() : null,
                cot != null ? cot.getId() : null,
                cot != null ? cot.getCostoArticulos() : null,
                cot != null ? cot.getCostoFlete() : null,
                cot != null ? cot.getCostoArmado() : null,
                cot != null ? cot.getTotal() : null,
                r.getMontoAnticipo(),
                r.getEstado(),
                r.getFechaReserva(),
                r.getFechaLimitePago(),
                r.getFechaConfirmacion(),
                r.getEmpleadoAsignadoId()
        );
    }

    @Transactional(readOnly = true)
    public Page<ReservaResponse> findReservasAdmin(
            String nombreCliente,
            String estado,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin,
            Pageable pageable
    ) {
        return reservaRepository.findAllWithFilters(nombreCliente, estado, fechaInicio, fechaFin, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<ReservaResponse> findReservasEmpleado(
            String nombreCliente,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin,
            Pageable pageable
    ) {
        List<String> estadosActivos = List.of("CONFIRMADA", "PENDIENTE_PAGO");
        return reservaRepository.findActiveWithFilters(estadosActivos, nombreCliente, fechaInicio, fechaFin, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<ReservaResponse> findReservasCliente(
            Long usuarioId,
            String estado,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin,
            Pageable pageable
    ) {
        return reservaRepository.findByUsuarioIdWithFilters(usuarioId, estado, fechaInicio, fechaFin, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public byte[] generarReciboPdf(Long reservaId) {
        Reserva r = reservaRepository.findById(reservaId)
                .orElseThrow(ReservaNoEncontradaException::new);

        Cotizacion cot = r.getCotizacion();
        if (cot == null) {
            throw new AppException(ErrorCode.ERROR_INTERNO, "La reserva no contiene una cotización asociada");
        }
        ProyectoDiseno proj = cot.getProyectoDiseno();
        Usuario usr = r.getUsuario();

        // 1. Obtener configuraciones del sistema
        String empNombre = configuracionService.findByClave("EMPRESA_NOMBRE").getValor();
        String empNit = configuracionService.findByClave("EMPRESA_NIT").getValor();
        String empDireccion = configuracionService.findByClave("EMPRESA_DIRECCION").getValor();
        String empTelefono = configuracionService.findByClave("EMPRESA_TELEFONO").getValor();
        String empEmail = configuracionService.findByClave("EMPRESA_EMAIL").getValor();
        String recNotas = configuracionService.findByClave("RECIBO_PI_PAGINA").getValor();

        // 2. Obtener datos de pago
        List<Pago> pagos = pagoRepository.findAllByReservaId(reservaId);
        Pago pagoReciente = pagos.stream()
                .filter(p -> "COMPLETADO".equalsIgnoreCase(p.getEstado()))
                .findFirst()
                .orElse(null);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A5, 30, 30, 30, 30);
            PdfWriter.getInstance(document, out);
            document.open();

            // Fuentes
            Font companyFont = new Font(Font.HELVETICA, 16, Font.BOLD, new java.awt.Color(236, 72, 153));
            Font titleFont = new Font(Font.HELVETICA, 12, Font.BOLD, java.awt.Color.DARK_GRAY);
            Font labelFont = new Font(Font.HELVETICA, 8, Font.BOLD, java.awt.Color.BLACK);
            Font valFont = new Font(Font.HELVETICA, 8, Font.NORMAL, java.awt.Color.BLACK);
            Font noteFont = new Font(Font.HELVETICA, 7, Font.ITALIC, java.awt.Color.GRAY);

            // Cabecera Empresa
            Paragraph pHeader = new Paragraph(empNombre.toUpperCase(), companyFont);
            pHeader.setAlignment(Element.ALIGN_CENTER);
            document.add(pHeader);

            Paragraph pSub = new Paragraph(String.format("NIT: %s | Tel: %s\n%s\n%s", empNit, empTelefono, empDireccion, empEmail), new Font(Font.HELVETICA, 8, Font.NORMAL, java.awt.Color.GRAY));
            pSub.setAlignment(Element.ALIGN_CENTER);
            document.add(pSub);

            document.add(new Paragraph("\n"));

            // Título del Recibo
            Paragraph pTitle = new Paragraph("RECIBO DE ANTICIPO DE RESERVA", titleFont);
            pTitle.setAlignment(Element.ALIGN_CENTER);
            document.add(pTitle);

            Paragraph pResId = new Paragraph(String.format("Código de Reserva: #%d  |  Fecha: %s", r.getId(), r.getFechaReserva().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))), valFont);
            pResId.setAlignment(Element.ALIGN_CENTER);
            document.add(pResId);

            document.add(new Paragraph("\n"));

            // Tabla de Detalles
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.5f, 3.5f});

            addCell(table, "CLIENTE:", labelFont);
            addCell(table, usr.getNombreCompleto(), valFont);

            addCell(table, "CORREO:", labelFont);
            addCell(table, usr.getEmail(), valFont);

            addCell(table, "TELÉFONO:", labelFont);
            addCell(table, usr.getTelefono() != null ? usr.getTelefono() : "No registrado", valFont);

            addCell(table, "PROYECTO:", labelFont);
            addCell(table, proj.getNombre(), valFont);

            addCell(table, "FECHA EVENTO:", labelFont);
            addCell(table, proj.getFechaEvento() != null ? proj.getFechaEvento().toString() : "No programada", valFont);

            addCell(table, "LUGAR EVENTO:", labelFont);
            addCell(table, proj.getLugarEvento() != null ? proj.getLugarEvento() : "No definido", valFont);

            addCell(table, "ESTADO RESERVA:", labelFont);
            addCell(table, r.getEstado(), valFont);

            addCell(table, "TOTAL PRESUPUESTO:", labelFont);
            addCell(table, "Bs. " + String.format("%.2f", cot.getTotal()), valFont);

            addCell(table, "ANTICIPO (20%):", labelFont);
            addCell(table, "Bs. " + String.format("%.2f", r.getMontoAnticipo()), labelFont);

            if (pagoReciente != null) {
                addCell(table, "MONTO PAGADO:", labelFont);
                addCell(table, "Bs. " + String.format("%.2f", pagoReciente.getMonto()), valFont);

                addCell(table, "MÉTODO DE PAGO:", labelFont);
                addCell(table, pagoReciente.getMetodoPago() != null ? pagoReciente.getMetodoPago().getNombre() : "No especificado", valFont);

                addCell(table, "REF. TRANSACCIÓN:", labelFont);
                addCell(table, pagoReciente.getReferenciaExterna(), valFont);

                addCell(table, "FECHA DE PAGO:", labelFont);
                addCell(table, pagoReciente.getFechaPago().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), valFont);
            }

            document.add(table);

            document.add(new Paragraph("\n\n"));

            // Notas
            Paragraph pNotes = new Paragraph(recNotas, noteFont);
            pNotes.setAlignment(Element.ALIGN_CENTER);
            document.add(pNotes);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error al generar el recibo PDF: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.ERROR_INTERNO, "No se pudo generar el recibo en PDF");
        }
    }

    private void addCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        cell.setBorderColor(java.awt.Color.LIGHT_GRAY);
        table.addCell(cell);
    }
}
