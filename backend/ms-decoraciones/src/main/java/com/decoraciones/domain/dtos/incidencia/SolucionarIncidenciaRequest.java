package com.decoraciones.domain.dtos.incidencia;

import java.math.BigDecimal;

public record SolucionarIncidenciaRequest(
    BigDecimal costoReparacion
) {
}
