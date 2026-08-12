package com.logistics.orderservice.application.exception;

public class StockDecreaseUnknownException extends  RuntimeException {
    public StockDecreaseUnknownException(String message) {
        super(message);
    }

    public StockDecreaseUnknownException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
