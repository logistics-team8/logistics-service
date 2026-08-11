package com.logistics.notificationservice.domain.common.exception;

import com.logistics.common.error.ErrorCode;
import org.apache.http.protocol.HTTP;
import org.springframework.http.HttpStatus;

public enum NotificationErrorCode implements ErrorCode {

    SLACK_RESPONSE_EMPTY(
            "NOTIFICATION_001",
            HttpStatus.BAD_GATEWAY,
            "Slack 응답이 없습니다."
    ),

    SLACK_SEND_FAILED(
            "NOTIFICATION_002",
            HttpStatus.BAD_GATEWAY,
            "Slack 메시지 전송에 실패했습니다."
    ),

    GEMINI_RESPONSE_EMPTY(
            "NOTIFICATION_101",
            HttpStatus.BAD_GATEWAY,
            "Gemini 응답이 없습니다."
    ),

    GEMINI_RESPONSE_CONTENT_EMPTY(
            "NOTIFICATION_102",
            HttpStatus.BAD_GATEWAY,
            "Gemini 응답 내용이 없습니다."
    ),

    GEMINI_RESPONSE_PARSE_FAILED(
            "NOTIFICATION_103",
            HttpStatus.BAD_GATEWAY,
            "Gemini 응답을 변환할 수 없습니다."
    ),

    USER_NOT_FOUND(
            "INTERNAL_001",
            HttpStatus.BAD_GATEWAY,
            "사용자의 Slack_ID를 조회할 수 없습니다."
    );



    private final String code;
    private final HttpStatus status;
    private final String message;

    NotificationErrorCode(
            String code,
            HttpStatus status,
            String message
    ) {
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