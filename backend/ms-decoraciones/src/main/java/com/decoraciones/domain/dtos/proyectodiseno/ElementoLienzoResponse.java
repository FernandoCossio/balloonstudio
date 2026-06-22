package com.decoraciones.domain.dtos.proyectodiseno;

import com.decoraciones.domain.dtos.articuloinventario.ImagenArticuloResponse;
import java.math.BigDecimal;
import java.util.List;

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
        String layer,
        String vistaActual,
        List<ImagenArticuloResponse> imagenes
) {}
