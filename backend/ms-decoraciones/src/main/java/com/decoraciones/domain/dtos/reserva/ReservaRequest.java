package com.decoraciones.domain.dtos.reserva;

import jakarta.validation.constraints.NotNull;

public record ReservaRequest(
    @NotNull(message = "El ID de usuario es obligatorio")
    Long usuarioId
) {
}
