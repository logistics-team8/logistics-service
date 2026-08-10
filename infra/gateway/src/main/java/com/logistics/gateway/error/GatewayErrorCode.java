package com.logistics.gateway.error;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum GatewayErrorCode implements ErrorCode {
    UNAUTHORIZED("GATEWAY_001", HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    TOKEN_INVALID("GATEWAY_002", HttpStatus.UNAUTHORIZED, "유효하지 않은 인증 정보입니다."),
    TOKEN_EXPIRED("GATEWAY_003", HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
    INVALID_INPUT("GATEWAY_004", HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    RESOURCE_NOT_FOUND("GATEWAY_201", HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    USER_NOT_FOUND("GATEWAY_202", HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR("GATEWAY_999", HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;

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
