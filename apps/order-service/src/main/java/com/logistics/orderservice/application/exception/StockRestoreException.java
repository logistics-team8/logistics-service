package com.logistics.orderservice.application.exception;

public class StockRestoreException extends RuntimeException {
    public StockRestoreException(String message) {
        super(message);
    }

    public StockRestoreException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
