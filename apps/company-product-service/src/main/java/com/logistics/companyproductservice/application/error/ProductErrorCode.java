package com.logistics.companyproductservice.application.error;

import com.logistics.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum ProductErrorCode implements ErrorCode {
    INSUFFICIENT_STOCK("PROD_301", HttpStatus.CONFLICT, "재고가 부족합니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;

    ProductErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }

    @Override public String code() { return code; }
    @Override public HttpStatus status() { return status; }
    @Override public String message() { return message; }
}