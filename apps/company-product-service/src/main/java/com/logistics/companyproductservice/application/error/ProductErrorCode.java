package com.logistics.companyproductservice.application.error;

import com.logistics.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum ProductErrorCode implements ErrorCode {
    INSUFFICIENT_STOCK("PROD_301", HttpStatus.CONFLICT, "재고가 부족합니다."),
    NOT_OWNED_PRODUCT("PROD_302", HttpStatus.FORBIDDEN, "본인 소속 업체 또는 담당 허브의 상품만 관리할 수 있습니다.");

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