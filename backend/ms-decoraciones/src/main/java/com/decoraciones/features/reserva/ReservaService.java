package com.decoraciones.features.reserva;

import com.decoraciones.common.errors.*;
import com.decoraciones.domain.dtos.reserva.ReservaRequest;
import com.decoraciones.domain.models.*;
import com.decoraciones.features.elementolienzo.ElementoLienzoRepository;
import com.decoraciones.features.inventario.BloqueoInventarioRepository;
import com.decoraciones.features.pago.PagoRepository;
import com.decoraciones.features.pago.StripeService;
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

    @org.springframework.beans.factory.annotation.Value("${app.reserva.lock-ttl-minutes:15}")
    private int lockTtlMinutes;

    /**
     * Inicia el flujo de reserva: genera cotización, reserva en PENDIENTE, bloquea temporalmente en Redis y crea PaymentIntent en Stripe.
     */
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

        // 2. Calcular costos de los artículos en el canvas para todos los escenarios del proyecto
        List<ElementoLienzo> elementos = elementoRepository.findAllByProyectoIdOrderByZIndexAsc(proyectoId);
        BigDecimal costoArticulos = BigDecimal.ZERO;
        for (ElementoLienzo el : elementos) {
            BigDecimal precioUnitario = el.getArticuloInventario().getCostoAdquisicion()
                    .multiply(BigDecimal.ONE.add(el.getArticuloInventario().getPorcentajeGanancia().divide(BigDecimal.valueOf(100))));
            costoArticulos = costoArticulos.add(precioUnitario.multiply(BigDecimal.valueOf(el.getCantidad())));
        }

        // Simulación de cotización simple (puedes expandir fletes y armado)
        BigDecimal costoFlete = BigDecimal.valueOf(50.00);
        BigDecimal costoArmado = BigDecimal.valueOf(100.00);
        BigDecimal total = costoArticulos.add(costoFlete).add(costoArmado);

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
                    true // strict = true
            );
            if (!lockAdquirido) {
                // Si falla un bloqueo, liberamos los bloqueos de este proyecto en Redis
                lockService.liberarBloqueosTemporales(proyectoId, true);
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
        cotizacion.setTasaOverheadAplicada(BigDecimal.valueOf(10.00));
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
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(ReservaNoEncontradaException::new);

        if (!"PENDIENTE_PAGO".equals(reserva.getEstado())) {
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
        pago.setMetodo("STRIPE");
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

        log.info("Pago confirmado exitosamente para la reserva ID: {}. Bloqueos permanentes consolidados.", reservaId);
    }
}
