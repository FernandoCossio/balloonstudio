package com.decoraciones.common.errors;

public class ImagenNoPerteneceAlArticuloException extends AppException {
    public ImagenNoPerteneceAlArticuloException() {
        super(ErrorCode.IMAGEN_NO_PERTENECE_AL_ARTICULO);
    }
}
