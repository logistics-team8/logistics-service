package com.logistics.orderservice.application.exception;

public class DeliveryStatusUnknownException extends RuntimeException {
    public DeliveryStatusUnknownException(String message) {
        super(message);
    }

    public DeliveryStatusUnknownException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
