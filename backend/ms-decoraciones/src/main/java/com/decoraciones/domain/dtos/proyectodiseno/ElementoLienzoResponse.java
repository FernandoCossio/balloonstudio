package com.decoraciones.domain.dtos.proyectodiseno;

import java.math.BigDecimal;

public record ElementoLienzoResponse(
        Long id,
        Long articuloId,
        String nombreArticulo,
        String imagenUrl,           // URL pública construida por el mapper
        BigDecimal costoAdquisicion,
        BigDecimal porcentajeGanancia,
        Integer cantidad,
        Double posX,
        Double posY,
        Double width,
        Double height,
        Double scaleX,
        Double scaleY,
        Double rotacionDeg,
        Double opacity,
        Integer zIndex,
        String layer
) {}
