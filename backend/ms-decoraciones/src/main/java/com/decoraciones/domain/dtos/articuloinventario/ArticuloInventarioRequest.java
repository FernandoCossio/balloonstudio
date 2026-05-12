package com.decoraciones.domain.dtos.articuloinventario;

import java.math.BigDecimal;
import java.util.List;

public record ArticuloInventarioRequest(
        String nombre,
        String descripcion,
        String tipoArticulo,
        String estado,
        BigDecimal costoAdquisicion,
        BigDecimal porcentajeGanancia,
        BigDecimal valorResidual,
        Integer vidaUtilAnos,
        Integer vidaUtilUsos,
        Integer stockTotal,
        BigDecimal pesoKg,
        BigDecimal volumenM3,
        Integer tiempoArmadoMin,
        Integer diasPreparacionPrevios,
        Integer diasLimpiezaPosteriores,
        BigDecimal mantenimientoPromedioBs,
        String nivelComplejidad,
        String embeddingVisual,
        List<Long> categoriaIds
) {}
