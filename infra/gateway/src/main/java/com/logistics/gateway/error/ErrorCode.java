package com.logistics.gateway.presentation.error;

import org.springframework.http.HttpStatus;

public interface ErrorCode {
    String code();

    HttpStatus status();

    String message();
}
