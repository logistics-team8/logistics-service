package com.logistics.hubservice.application.hub;

import com.logistics.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum HubErrorCode implements ErrorCode {
    HUB_NOT_FOUND("HUB_001", HttpStatus.NOT_FOUND, "허브를 찾을 수 없습니다."),
    HUB_ROUTE_DUPLICATE("HUB_003", HttpStatus.CONFLICT, "이미 등록된 허브 경로입니다."),
    HUB_ROUTE_SAME_HUB("HUB_004", HttpStatus.BAD_REQUEST, "출발 허브와 도착 허브는 달라야 합니다.");

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
