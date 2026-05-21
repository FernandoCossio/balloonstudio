package com.decoraciones.domain.dtos.proyectodiseno;

import java.time.LocalDate;

public record ProyectoDisenoRequest(
        String nombre,
        String descripcion,
        String estado,
        LocalDate fechaEvento,
        String lugarEvento,
        String numeroMetadato
) {}
