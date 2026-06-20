package com.decoraciones.domain.dtos.empleado;

import java.util.Set;

public record EmpleadoResponse(
        Long id,
        String username,
        String nombreCompleto,
        String email,
        String telefono,
        Boolean activo,
        Set<String> roles
) {}
