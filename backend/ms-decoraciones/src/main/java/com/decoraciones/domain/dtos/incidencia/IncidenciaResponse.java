package com.decoraciones.domain.dtos.incidencia;

import com.decoraciones.domain.dtos.articuloinventario.ArticuloInventarioResponse;
import java.math.BigDecimal;
import java.time.LocalDate;

public record IncidenciaResponse(
    Long id,
    ArticuloInventarioResponse articuloInventario,
    Long reservaId,
    String descripcion,
    String tipo,
    String estado,
    Integer cantidadAfectada,
    LocalDate fechaIncidencia,
    LocalDate fechaResolucionEstimada,
    BigDecimal costoReparacion
) {}
