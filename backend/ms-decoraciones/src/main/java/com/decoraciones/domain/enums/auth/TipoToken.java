package com.decoraciones.domain.enums.auth;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TipoToken {
    ACTIVACION_CUENTA("Activación de cuenta"),
    RECUPERAR_PASSWORD("Recuperación de contraseña"),
    DOS_FACTORES("Segundo factor de autenticación");

    private final String descripcion;
}
