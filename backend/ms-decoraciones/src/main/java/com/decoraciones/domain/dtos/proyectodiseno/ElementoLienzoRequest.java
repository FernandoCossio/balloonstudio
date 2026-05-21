package com.decoraciones.domain.dtos.proyectodiseno;

public record ElementoLienzoRequest(
        Long articuloId,
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
        String layer           // "mid" | "main"
) {}
