package com.decoraciones.domain.dtos.proyectodiseno;

public record EscenarioBaseRequest(
        String nombre,
        String descripcion,
        Integer dimensionesAltoPx,
        Integer dimensionesAnchoPx
) {}
