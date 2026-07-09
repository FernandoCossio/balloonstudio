package com.decoraciones.features.reserva;

import com.decoraciones.common.decorators.CurrentUserId;
import com.decoraciones.common.response.ApiResponse;
import com.decoraciones.domain.dtos.reserva.ReservaRequest;
import com.decoraciones.domain.dtos.reserva.ReservaResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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
            Long proyectoId = Long.parseLong(cleanPedidoId);
            String estado = request.Estado();
            
            if ("2".equals(estado) || "Pagado".equalsIgnoreCase(estado) || "5".equals(estado) || "Revisión".equalsIgnoreCase(estado)) {
                reservaService.confirmarPago(proyectoId, "CALLBACK_PF_" + rawPedidoId, "PAGO_FACIL");
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

    private Pageable createPageable(int page, int size, String[] sort) {
        Sort.Direction direction = Sort.Direction.DESC;
        String property = "fechaReserva";
        if (sort != null && sort.length >= 2) {
            property = sort[0];
            if ("asc".equalsIgnoreCase(sort[1])) {
                direction = Sort.Direction.ASC;
            }
        } else if (sort != null && sort.length == 1) {
            property = sort[0];
        }
        return PageRequest.of(page, size, Sort.by(direction, property));
    }

    @GetMapping("/reservas/admin")
    public ResponseEntity<ApiResponse<Page<ReservaResponse>>> getReservasAdmin(
            @RequestParam(required = false) String nombreCliente,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "fechaReserva,desc") String[] sort
    ) {
        Pageable pageable = createPageable(page, size, sort);
        Page<ReservaResponse> result = reservaService.findReservasAdmin(nombreCliente, estado, fechaInicio, fechaFin, pageable);
        return ResponseEntity.ok(ApiResponse.success(result, "Reservas (Admin) obtenidas correctamente."));
    }

    @GetMapping("/reservas/empleado")
    public ResponseEntity<ApiResponse<Page<ReservaResponse>>> getReservasEmpleado(
            @RequestParam(required = false) String nombreCliente,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "fechaReserva,desc") String[] sort
    ) {
        Pageable pageable = createPageable(page, size, sort);
        Page<ReservaResponse> result = reservaService.findReservasEmpleado(nombreCliente, fechaInicio, fechaFin, pageable);
        return ResponseEntity.ok(ApiResponse.success(result, "Reservas activas (Empleado) obtenidas correctamente."));
    }

    @GetMapping("/reservas/cliente")
    public ResponseEntity<ApiResponse<Page<ReservaResponse>>> getReservasCliente(
            @CurrentUserId Long usuarioId,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "fechaReserva,desc") String[] sort
    ) {
        Pageable pageable = createPageable(page, size, sort);
        Page<ReservaResponse> result = reservaService.findReservasCliente(usuarioId, estado, fechaInicio, fechaFin, pageable);
        return ResponseEntity.ok(ApiResponse.success(result, "Sus reservas (Cliente) obtenidas correctamente."));
    }

    @GetMapping("/reservas/{reservaId}/recibo")
    public ResponseEntity<byte[]> descargarRecibo(@PathVariable Long reservaId) {
        byte[] pdfBytes = reservaService.generarReciboPdf(reservaId);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "recibo_reserva_" + reservaId + ".pdf");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
}

