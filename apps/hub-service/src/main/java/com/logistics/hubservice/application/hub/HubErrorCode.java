package com.logistics.hubservice.application.hub;

import com.logistics.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum HubErrorCode implements ErrorCode {
    HUB_NOT_FOUND("HUB_001", HttpStatus.NOT_FOUND, "허브를 찾을 수 없습니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;

    HubErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public HttpStatus status() {
        return status;
    }

    @Override
    public String message() {
        return message;
    }
}
