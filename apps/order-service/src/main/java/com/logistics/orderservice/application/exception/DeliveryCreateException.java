package com.logistics.orderservice.application.exception;

public class DeliveryCreateException extends RuntimeException {

    public DeliveryCreateException(String message) {
        super(message);
    }

    public DeliveryCreateException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
