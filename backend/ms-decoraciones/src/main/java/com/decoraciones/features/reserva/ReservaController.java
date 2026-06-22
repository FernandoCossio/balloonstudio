package com.decoraciones.features.reserva;

import com.decoraciones.common.response.ApiResponse;
import com.decoraciones.domain.dtos.reserva.ReservaRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/proyectos")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService reservaService;

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
            @RequestParam("referenciaPago") String referenciaPago) {

        reservaService.confirmarPago(reservaId, referenciaPago);
        return ResponseEntity.ok(ApiResponse.success(null, "Pago y reserva confirmados correctamente."));
    }
}
