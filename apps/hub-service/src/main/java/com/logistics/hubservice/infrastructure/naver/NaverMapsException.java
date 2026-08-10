package com.logistics.hubservice.infrastructure.naver;

public class NaverMapsException extends RuntimeException {

    public NaverMapsException(String message) {
        super(message);
    }

    public NaverMapsException(String message, Throwable cause) {
        super(message, cause);
    }
}
