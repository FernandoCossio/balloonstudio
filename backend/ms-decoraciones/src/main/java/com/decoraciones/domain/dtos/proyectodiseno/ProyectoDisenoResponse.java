package com.decoraciones.domain.dtos.proyectodiseno;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ProyectoDisenoResponse(
        Long id,
        String nombre,
        String descripcion,
        String estado,
        LocalDate fechaEvento,
        String lugarEvento,
        String numeroMetadato,
        Double distanciaKm,
        Double latitud,
        Double longitud,
        BigDecimal costoRealTotal,
        Long escenarioBaseId,       // escenario activo por defecto
        LocalDateTime fechaCreacion,
        LocalDateTime fechaUltimaModificacion,
        List<EscenarioBaseResponse> escenarios
) {}
