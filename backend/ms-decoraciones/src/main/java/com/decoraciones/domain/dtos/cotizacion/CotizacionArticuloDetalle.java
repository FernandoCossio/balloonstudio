package com.decoraciones.domain.dtos.cotizacion;

import java.math.BigDecimal;

public record CotizacionArticuloDetalle(
        Long articuloId,
        String nombre,
        Integer cantidad,
        BigDecimal precioUnitario,
        BigDecimal precioTotal,
        String tipoArticulo,
        String nivelComplejidad
) {}
