package com.logistics.orderservice.application.exception;

public class DeliveryLookupException extends RuntimeException {
    public DeliveryLookupException(String message) {
        super(message);
    }

    public DeliveryLookupException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
