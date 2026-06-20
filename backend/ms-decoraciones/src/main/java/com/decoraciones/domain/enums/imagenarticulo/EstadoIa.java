package com.decoraciones.domain.enums.imagenarticulo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EstadoIa {
    PENDIENTE("Pendiente"),
    PROCESANDO("Procesando"),
    PROCESADO("Procesado"),
    FALLIDO("Fallido");

    private final String descripcion;
}
