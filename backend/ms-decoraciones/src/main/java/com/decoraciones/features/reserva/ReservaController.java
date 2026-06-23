package com.decoraciones.features.reserva;

import com.decoraciones.common.response.ApiResponse;
import com.decoraciones.domain.dtos.reserva.ReservaRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/proyectos")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService reservaService;

    public record PagoFacilQrRequest(
            String nombreCliente,
            String ciCliente,
            String telefonoCliente,
            String correoCliente
    ) {}

    public record PagoFacilCallbackRequest(
            String PedidoID,
            String Fecha,
            String Hora,
            String MetodoPago,
            String Estado
    ) {}

    /**
     * Inicia el flujo de reserva: genera cotización, reserva en PENDIENTE, bloquea temporalmente en Redis y crea PaymentIntent en Stripe.
     */
    @PostMapping("/{proyectoId}/reservar")
    public ResponseEntity<ApiResponse<Map<String, Object>>> iniciarReserva(
            @PathVariable Long proyectoId,
            @RequestBody @Valid ReservaRequest request) {
        
        Map<String, Object> data = reservaService.iniciarReserva(proyectoId, request);
        return ResponseEntity.ok(ApiResponse.success(data, "Reserva iniciada correctamente. Complete el pago en los próximos 15 minutos."));
    }

    /**
     * Webhook de Stripe o Pago Fácil que confirma la transacción.
     * Consolida la reserva en PostgreSQL y elimina los bloqueos de Redis.
     */
    @PostMapping("/pagos/webhook")
    public ResponseEntity<ApiResponse<Void>> confirmarPago(
            @RequestParam("reservaId") Long reservaId,
            @RequestParam("referenciaPago") String referenciaPago,
            @RequestParam(value = "metodoPago", required = false, defaultValue = "STRIPE") String metodoPago) {

        reservaService.confirmarPago(reservaId, referenciaPago, metodoPago);
        return ResponseEntity.ok(ApiResponse.success(null, "Pago y reserva confirmados correctamente."));
    }

    @PostMapping("/reservas/{reservaId}/qr")
    public ResponseEntity<ApiResponse<com.decoraciones.features.pago.PagoFacilService.PagoFacilQrResponse>> generarQrPago(
            @PathVariable Long reservaId,
            @RequestBody PagoFacilQrRequest request) {
        
        log.info("Petición para generar QR PagoFacil de reserva: {}", reservaId);
        com.decoraciones.features.pago.PagoFacilService.PagoFacilQrResponse response = reservaService.generarQrPago(
                reservaId,
                request.nombreCliente(),
                request.ciCliente(),
                request.correoCliente(),
                request.telefonoCliente()
        );
        return ResponseEntity.ok(ApiResponse.success(response, "Código QR generado exitosamente por PagoFácil."));
    }

    @GetMapping("/reservas/{reservaId}/pago-estado")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verificarEstadoPagoQr(
            @PathVariable Long reservaId,
            @RequestParam("transactionId") String transactionId) {
        
        boolean pagado = reservaService.verificarEstadoPagoQr(reservaId, transactionId);
        Map<String, Object> result = Map.of(
                "reservaId", reservaId,
                "transactionId", transactionId,
                "pagado", pagado
        );
        return ResponseEntity.ok(ApiResponse.success(result, pagado ? "El pago ha sido completado." : "El pago sigue pendiente."));
    }

    @PostMapping("/pagos/pagofacil-callback")
    public ResponseEntity<Map<String, Object>> pagofacilCallback(
            @RequestBody PagoFacilCallbackRequest request) {
        
        log.info("Recibida notificación de Callback PagoFacil: {}", request);
        try {
            String rawPedidoId = request.PedidoID();
            String cleanPedidoId = rawPedidoId;
            if (rawPedidoId != null && rawPedidoId.contains("-")) {
                cleanPedidoId = rawPedidoId.split("-")[0];
            }
            Long reservaId = Long.parseLong(cleanPedidoId);
            String estado = request.Estado();
            
            if ("2".equals(estado) || "Pagado".equalsIgnoreCase(estado) || "5".equals(estado) || "Revisión".equalsIgnoreCase(estado)) {
                reservaService.confirmarPago(reservaId, "CALLBACK_PF_" + rawPedidoId, "PAGO_FACIL");
            }
            
            Map<String, Object> successResponse = Map.of(
                    "error", 0,
                    "status", 1,
                    "message", "Pago confirmado exitosamente en el callback",
                    "values", true
            );
            return ResponseEntity.ok(successResponse);
        } catch (Exception e) {
            log.error("Error al procesar callback de PagoFacil: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = Map.of(
                    "error", 1,
                    "status", 0,
                    "message", "Error al procesar callback: " + e.getMessage(),
                    "values", false
            );
            return ResponseEntity.ok(errorResponse);
        }
    }
}

