package com.logistics.orderservice.application.exception;

public class StockRestoreUnknownException extends RuntimeException {
    public StockRestoreUnknownException(String message) {
        super(message);
    }

    public StockRestoreUnknownException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
