package com.logistics.gateway.presentation.error;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum GatewayErrorCode implements ErrorCode {
    UNAUTHORIZED("GATEWAY_001", HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    TOKEN_INVALID("GATEWAY_002", HttpStatus.UNAUTHORIZED, "유효하지 않은 인증 정보입니다."),
    TOKEN_EXPIRED("GATEWAY_003", HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
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
