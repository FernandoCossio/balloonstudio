package com.decoraciones.features.reserva;

import com.decoraciones.common.errors.*;
import com.decoraciones.domain.dtos.reserva.ReservaRequest;
import com.decoraciones.domain.models.*;
import com.decoraciones.features.elementolienzo.ElementoLienzoRepository;
import com.decoraciones.features.inventario.BloqueoInventarioRepository;
import com.decoraciones.features.pago.PagoRepository;
import com.decoraciones.features.pago.StripeService;
import com.decoraciones.features.pago.PagoFacilService;
import com.decoraciones.features.proyectodiseno.ProyectoDisenoRepository;
import com.decoraciones.features.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final InventarioLockService lockService;
    private final StripeService stripeService;
    private final UsuarioRepository usuarioRepository;
    private final CotizacionService cotizacionService;
    private final PagoFacilService pagoFacilService;

    @org.springframework.beans.factory.annotation.Value("${app.reserva.lock-ttl-minutes:15}")
    private int lockTtlMinutes;
// ... (iniciarReserva stays exactly same) ...
    @Transactional
    public Map<String, Object> iniciarReserva(Long proyectoId, ReservaRequest request) {
        ProyectoDiseno proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(ProyectoDisenoNoEncontradoException::new);

        Usuario usuario = usuarioRepository.findById(request.usuarioId())
                .orElseThrow(UsuarioNoEncontradoException::new);

        LocalDate fechaEvento = proyecto.getFechaEvento() != null ? proyecto.getFechaEvento() : LocalDate.now().plusDays(30);

        // 1. Cancelar reservas anteriores en PENDIENTE_PAGO del mismo proyecto
        List<Reserva> reservasPendientes = reservaRepository.findAllByCotizacionProyectoDisenoIdAndEstado(proyectoId, "PENDIENTE_PAGO");
        for (Reserva r : reservasPendientes) {
            r.setEstado("EXPIRADA");
            reservaRepository.save(r);
            log.info("Cancelada reserva pendiente previa ID: {} para el proyecto ID: {}", r.getId(), proyectoId);
        }

        // 2. Calcular costos utilizando el motor de cotización real
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

        // 3. Bloqueo temporal en Redis (Estricto - falla si Redis está caído)
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

        // 4. Crear Cotizacion
        Cotizacion cotizacion = new Cotizacion();
        cotizacion.setProyectoDiseno(proyecto);
        cotizacion.setCostoArticulos(costoArticulos);
        cotizacion.setCostoFlete(costoFlete);
        cotizacion.setCostoArmado(costoArmado);
        cotizacion.setTotal(total);
        cotizacion.setFechaGeneracion(LocalDateTime.now());
        cotizacion.setTasaOverheadAplicada(detail.tasaOverheadAplicada());
        cotizacion = cotizacionRepository.save(cotizacion);

        // 5. Crear Reserva en estado PENDIENTE_PAGO
        BigDecimal porcentajeAnticipo = BigDecimal.valueOf(0.20); // 20%
        BigDecimal montoAnticipo = total.multiply(porcentajeAnticipo);

        Reserva reserva = new Reserva();
        reserva.setCotizacion(cotizacion);
        reserva.setUsuario(usuario);
        reserva.setEstado("PENDIENTE_PAGO");
        reserva.setFechaReserva(LocalDateTime.now());
        reserva.setFechaLimitePago(LocalDateTime.now().plusMinutes(lockTtlMinutes));
        reserva.setMontoAnticipo(montoAnticipo);
        reserva = reservaRepository.save(reserva);

        // 6. Crear PaymentIntent en Stripe
        String clientSecret = stripeService.crearPaymentIntent(montoAnticipo, "BOB");

        Map<String, Object> data = new HashMap<>();
        data.put("reservaId", reserva.getId());
        data.put("montoAnticipo", montoAnticipo);
        data.put("stripeClientSecret", clientSecret);
        data.put("expiraEnMinutos", lockTtlMinutes);
        data.put("totalOriginal", total);
        data.put("costoArticulos", costoArticulos);
        data.put("costoFlete", costoFlete);
        data.put("costoArmado", costoArmado);

        return data;
    }

    /**
     * Webhook de Stripe o Pago Fácil que confirma la transacción.
     * Consolida la reserva en PostgreSQL y elimina los bloqueos de Redis.
     */
    @Transactional
    public void confirmarPago(Long reservaId, String referenciaPago) {
        confirmarPago(reservaId, referenciaPago, "STRIPE");
    }

    /**
     * Webhook de Stripe o Pago Fácil que confirma la transacción.
     * Consolida la reserva en PostgreSQL y elimina los bloqueos de Redis.
     */
    @Transactional
    public void confirmarPago(Long reservaId, String referenciaPago, String metodoPago) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(ReservaNoEncontradaException::new);

        if (!"PENDIENTE_PAGO".equals(reserva.getEstado())) {
            if ("CONFIRMADA".equals(reserva.getEstado())) {
                log.info("La reserva ID: {} ya está CONFIRMADA previamente.", reservaId);
                return;
            }
            throw new EstadoReservaInvalidoException("La reserva no está en estado pendiente de pago");
        }

        // 1. Cambiar estado de la reserva
        reserva.setEstado("CONFIRMADA");
        reserva.setFechaConfirmacion(LocalDateTime.now());
        reservaRepository.save(reserva);

        // 2. Registrar el pago
        Pago pago = new Pago();
        pago.setReserva(reserva);
        pago.setMonto(reserva.getMontoAnticipo());
        pago.setMetodo(metodoPago != null ? metodoPago.toUpperCase() : "STRIPE");
        pago.setEstado("COMPLETADO");
        pago.setFechaPago(LocalDateTime.now());
        pago.setReferenciaExterna(referenciaPago);
        pago.setTipoPago("ANTICIPO");
        pagoRepository.save(pago);

        // 3. Trasladar bloqueos temporales de Redis a PostgreSQL permanentemente
        ProyectoDiseno proyecto = reserva.getCotizacion().getProyectoDiseno();
        List<ElementoLienzo> elementos = elementoRepository.findAllByProyectoIdOrderByZIndexAsc(proyecto.getId());
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

        // 4. Liberar llaves temporales de Redis
        lockService.liberarBloqueosTemporales(proyecto.getId());

        log.info("Pago confirmado exitosamente para la reserva ID: {} con el método {}. Bloqueos permanentes consolidados.", reservaId, metodoPago);
    }

    /**
     * Genera un QR de PagoFácil para el anticipo de una reserva.
     */
    @Transactional
    public PagoFacilService.PagoFacilQrResponse generarQrPago(
            Long reservaId,
            String clientName,
            String clientCi,
            String email,
            String phone
    ) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(ReservaNoEncontradaException::new);

        if (!"PENDIENTE_PAGO".equals(reserva.getEstado())) {
            throw new EstadoReservaInvalidoException("La reserva no está en estado pendiente de pago");
        }

        String token = pagoFacilService.login();
        double amount = reserva.getMontoAnticipo().doubleValue();

        String safeName = (clientName != null && !clientName.trim().isEmpty()) ? clientName.trim() : reserva.getUsuario().getNombreCompleto();
        if (safeName == null || safeName.trim().isEmpty()) {
            safeName = "Cliente";
        }

        String rawEmail = (email != null && !email.trim().isEmpty()) ? email.trim() : reserva.getUsuario().getEmail();
        String safeEmail = "";
        if (rawEmail != null && !rawEmail.trim().isEmpty() && rawEmail.contains("@")) {
            safeEmail = rawEmail.trim();
        }

        String rawPhone = (phone != null && !phone.trim().isEmpty()) ? phone.trim() : reserva.getUsuario().getTelefono();
        String safePhone = "75540850"; // default fallback phone
        if (rawPhone != null && !rawPhone.trim().isEmpty()) {
            String digits = rawPhone.replaceAll("[^0-9]", "");
            if (digits.length() >= 7) {
                safePhone = digits;
            }
        }

        String safeCi = (clientCi != null && !clientCi.trim().isEmpty()) ? clientCi.trim() : "1234567";

        return pagoFacilService.generarQR(
                token,
                reservaId + "-" + System.currentTimeMillis(),
                amount,
                safeName,
                safeCi,
                safeEmail,
                safePhone
        );
    }

    @Transactional
    public boolean verificarEstadoPagoQr(Long reservaId, String transactionId) {
        String token = pagoFacilService.login();
        boolean pagado = pagoFacilService.consultarTransaccion(token, transactionId);

        if (pagado) {
            log.info("PagoFacil confirmó el pago para reserva ID: {}, transactionId: {}. Confirmando reserva...", reservaId, transactionId);
            confirmarPago(reservaId, transactionId, "PAGO_FACIL");
        }

        return pagado;
    }
}
