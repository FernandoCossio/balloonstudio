package com.decoraciones.domain.dtos.articuloinventario;

import com.decoraciones.domain.dtos.categoria.CategoriaResponse;

import java.math.BigDecimal;
import java.util.List;

public record ArticuloInventarioResponse(
        Long id,
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
        Integer stockDisponible,
        BigDecimal pesoKg,
        BigDecimal volumenM3,
        Integer tiempoArmadoMin,
        Integer diasPreparacionPrevios,
        Integer diasLimpiezaPosteriores,
        BigDecimal mantenimientoPromedioBs,
        String nivelComplejidad,
        String embeddingVisual,
        List<CategoriaResponse> categorias,
        List<ImagenArticuloResponse> imagenes
) {}
