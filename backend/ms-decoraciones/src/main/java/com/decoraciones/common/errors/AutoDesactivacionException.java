package com.decoraciones.common.errors;

public class AutoDesactivacionException extends AppException {
    public AutoDesactivacionException() {
        super(ErrorCode.AUTO_DESACTIVACION_PROHIBIDA);
    }
}
