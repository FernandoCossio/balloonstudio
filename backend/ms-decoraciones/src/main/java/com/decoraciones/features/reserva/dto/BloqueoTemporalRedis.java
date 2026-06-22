package com.decoraciones.features.reserva.dto;

import java.io.Serializable;
import java.time.LocalDate;

public record BloqueoTemporalRedis(
        Long articuloId,
        Integer cantidad,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        Long proyectoId
) implements Serializable {}
