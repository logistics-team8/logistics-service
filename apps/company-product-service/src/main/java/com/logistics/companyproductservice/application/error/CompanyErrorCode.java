package com.logistics.companyproductservice.application.error;

import com.logistics.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum CompanyErrorCode implements ErrorCode {
    NOT_OWNED_COMPANY("COMPANY_301", HttpStatus.FORBIDDEN, "본인 소속 업체만 수정/삭제할 수 있습니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;

    CompanyErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }

    @Override public String code() { return code; }
    @Override public HttpStatus status() { return status; }
    @Override public String message() { return message; }
}