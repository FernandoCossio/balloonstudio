package com.decoraciones.domain.dtos.incidencia;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record IncidenciaRequest(
    @NotNull(message = "El ID del artículo es obligatorio")
    Long articuloId,

    Long reservaId,

    @NotBlank(message = "La descripción es obligatoria")
    String descripcion,

    @NotBlank(message = "El tipo de incidencia es obligatorio")
    String tipo,

    @NotNull(message = "La cantidad es obligatoria")
    @Positive(message = "La cantidad debe ser mayor a cero")
    Integer cantidad,

    String fechaResolucionEstimada
) {
}
