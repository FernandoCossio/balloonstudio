package com.decoraciones.common.errors;

public class EstadoReservaInvalidoException extends AppException {
    public EstadoReservaInvalidoException() {
        super(ErrorCode.ESTADO_RESERVA_INVALIDO);
    }

    public EstadoReservaInvalidoException(String message) {
        super(ErrorCode.ESTADO_RESERVA_INVALIDO, message);
    }
}
