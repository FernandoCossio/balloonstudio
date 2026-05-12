package com.decoraciones.domain.dtos.usuario;

import java.util.Set;
import java.util.UUID;

public record ResponseUsuarioDto(
    UUID uuid,
    String username,
    String email,
    String nombreCompleto,
    String telefono,
    Set<String> roles
) {
}
