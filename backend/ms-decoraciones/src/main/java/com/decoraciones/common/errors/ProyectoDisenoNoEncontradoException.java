package com.decoraciones.common.errors;

public class ProyectoDisenoNoEncontradoException extends RuntimeException {
    public ProyectoDisenoNoEncontradoException() {
        super("Proyecto de diseño no encontrado");
    }
}
