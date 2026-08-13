package com.logistics.orderservice.application.exception;

public class StockDecreaseException extends RuntimeException {
    public StockDecreaseException(String message) {
        super(message);
    }

    public StockDecreaseException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
