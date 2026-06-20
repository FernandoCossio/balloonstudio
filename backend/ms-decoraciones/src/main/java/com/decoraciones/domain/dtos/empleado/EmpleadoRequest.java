package com.decoraciones.domain.dtos.empleado;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmpleadoRequest(
        @NotBlank(message = "El nombre completo es obligatorio")
        String nombreCompleto,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email debe tener un formato válido")
        String email,

        String telefono,
        String username
) {}
