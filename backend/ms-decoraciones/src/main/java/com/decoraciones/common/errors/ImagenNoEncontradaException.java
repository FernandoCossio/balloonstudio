package com.decoraciones.common.errors;

public class ImagenNoEncontradaException extends AppException {
    public ImagenNoEncontradaException() {
        super(ErrorCode.IMAGEN_NO_ENCONTRADA);
    }
}
