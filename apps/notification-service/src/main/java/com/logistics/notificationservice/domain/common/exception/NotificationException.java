package com.logistics.notificationservice.domain.common.exception;

import com.logistics.common.error.ErrorCode;
import lombok.Getter;

@Getter
public class NotificationException extends RuntimeException {

    private final ErrorCode errorCode;


    public NotificationException(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    public NotificationException(
            ErrorCode errorCode,
            Throwable cause
    ) {
        super(errorCode.message(), cause);
        this.errorCode = errorCode;
    }
}