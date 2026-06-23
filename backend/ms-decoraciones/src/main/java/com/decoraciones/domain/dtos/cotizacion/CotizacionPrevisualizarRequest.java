package com.decoraciones.domain.dtos.cotizacion;

import com.decoraciones.domain.dtos.proyectodiseno.ElementoLienzoRequest;
import java.util.List;

public record CotizacionPrevisualizarRequest(
        Double distanciaKm,
        List<ElementoLienzoRequest> elementos
) {}
