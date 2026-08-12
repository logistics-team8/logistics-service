package com.logistics.orderservice.application.exception;

public class StockStatusLookupException extends RuntimeException{
    public StockStatusLookupException(String message) {
        super(message);
    }

    public StockStatusLookupException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
