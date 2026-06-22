package com.decoraciones.common.errors;

public class ReservaNoEncontradaException extends AppException {
    public ReservaNoEncontradaException() {
        super(ErrorCode.RESERVA_NO_ENCONTRADA);
    }
}
