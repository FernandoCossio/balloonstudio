package com.decoraciones.domain.dtos.proyectodiseno;

import java.util.List;

public record EscenarioBaseResponse(
        Long id,
        String nombre,
        String descripcion,
        String imagenUrl,
        String imagenDisenoUrl,
        Integer dimensionesAltoPx,
        Integer dimensionesAnchoPx,
        Boolean activo,
        List<ElementoLienzoResponse> elementos
) {}
