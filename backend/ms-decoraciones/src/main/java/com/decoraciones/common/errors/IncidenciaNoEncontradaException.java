package com.decoraciones.common.errors;

public class IncidenciaNoEncontradaException extends AppException {
    public IncidenciaNoEncontradaException() {
        super(ErrorCode.INCIDENCIA_NO_ENCONTRADA);
    }
}
