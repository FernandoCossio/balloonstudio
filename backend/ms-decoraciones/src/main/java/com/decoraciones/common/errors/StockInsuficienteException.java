package com.decoraciones.common.errors;

public class StockInsuficienteException extends AppException {
    public StockInsuficienteException() {
        super(ErrorCode.STOCK_INSUFICIENTE);
    }

    public StockInsuficienteException(String message) {
        super(ErrorCode.STOCK_INSUFICIENTE, message);
    }
}
