package com.decoraciones.domain.dtos.empleado;

import java.time.LocalDate;

public record EmpleadoRequest(
        String nombre,
        String apellido,
        String ci,
        String cargo,
        String telefono,
        String email,
        LocalDate fechaContratacion,
        Boolean activo
) {}
