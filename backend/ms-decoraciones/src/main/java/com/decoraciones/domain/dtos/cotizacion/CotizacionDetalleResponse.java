package com.decoraciones.domain.dtos.cotizacion;

import java.math.BigDecimal;
import java.util.List;

public record CotizacionDetalleResponse(
        BigDecimal costoArticulos,
        BigDecimal costoFlete,
        BigDecimal costoArmado,
        BigDecimal tasaOverheadAplicada,      // e.g. 10.00 (%)
        BigDecimal factorEstacionalAplicado,  // e.g. 1.20
        BigDecimal subtotal,                  // costoArticulos + flete + armado
        BigDecimal subtotalConOverhead,        // subtotal + overhead
        BigDecimal total,                     // subtotalConOverhead * factorEstacional
        Integer cantidadArticulos,
        List<CotizacionArticuloDetalle> desgloseArticulos
) {}
