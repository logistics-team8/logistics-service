package com.logistics.userservice.presentation.exception;

import com.logistics.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum AuthErrorCode implements ErrorCode {
    INVALID_LOGIN("AUTH_001", HttpStatus.UNAUTHORIZED, "아이디가 존재하지 않거나 비밀번호가 올바르지 않습니다."),

    TOKEN_EXPIRED("AUTH_101", HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
    TOKEN_INVALID("AUTH_102", HttpStatus.UNAUTHORIZED, "유효하지 않은 인증 정보입니다"),
    PENDING_APPROVAL("AUTH_103", HttpStatus.FORBIDDEN, "승인 대기중인 계정입니다."),
    APPROVAL_REJECTED("AUTH_104", HttpStatus.FORBIDDEN, "승인 거부된 계정입니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;

    AuthErrorCode(String code, HttpStatus status, String message) {
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
