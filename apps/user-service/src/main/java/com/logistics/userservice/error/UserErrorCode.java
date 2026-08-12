package com.logistics.userservice.error;

import com.logistics.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {
    USER_NOT_FOUND("USER_201", HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    USER_DUPLICATE_USERNAME("USER_202", HttpStatus.CONFLICT, "이미 사용중인 아이디입니다."),
    USER_DUPLICATE_SLACK_ID("USER_203", HttpStatus.CONFLICT, "이미 사용중인 Slack 아이디입니다.");

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
