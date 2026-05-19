package com.decoraciones.domain.dtos.proyectodiseno;

import java.math.BigDecimal;
import java.util.Set;

public record ArticuloInventarioDto(
        Long id,
        String nombre,
        String descripcion,
        String tipoArticulo,
        String estado,
        BigDecimal costoAdquisicion,
        BigDecimal porcentajeGanancia,
        Integer stockTotal,
        String imagenUrl,
        String imagenThumbnailUrl,
        Set<String> categorias
) {
}
