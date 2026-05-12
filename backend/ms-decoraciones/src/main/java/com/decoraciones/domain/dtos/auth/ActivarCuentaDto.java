package com.decoraciones.domain.dtos.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActivarCuentaDto(
    @NotBlank(message = "El token es obligatorio")
    String token,

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    String password,

    @NotBlank(message = "La confirmación de contraseña es obligatoria")
    String confirmPassword
) {
}
