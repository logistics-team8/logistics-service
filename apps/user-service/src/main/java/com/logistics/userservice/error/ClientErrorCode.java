package com.logistics.userservice.error;

import com.logistics.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum ClientErrorCode implements ErrorCode {
    HUB_ID_INVALID("CLIENT_001", HttpStatus.BAD_REQUEST, "유효하지 않은 허브 ID입니다."),
    COMPANY_ID_INVALID("CLIENT_002", HttpStatus.BAD_REQUEST, "유효하지 않은 업체 ID입니다."),
    DELIVERY_ID_INVALID("CLIENT_002", HttpStatus.BAD_REQUEST, "유효하지 않은 업체 ID입니다."),

    HUB_NOT_FOUND("CLIENT_201", HttpStatus.BAD_REQUEST, "존재하지 않는 허브입니다."),
    COMPANY_NOT_FOUND("CLIENT_202", HttpStatus.NOT_FOUND, "존재하지 않는 업체입니다."),
    DELIVERY_DUPLICATE_USER("CLIENT_203", HttpStatus.BAD_REQUEST, "이미 배송 담당자로 등록된 회원입니다."),

    SERVICE_UNAVAILABLE(
            "CLIENT_501",
            HttpStatus.SERVICE_UNAVAILABLE,
            "현재 서비스를 일시적으로 이용할 수 없습니다. 잠시 후 다시 시도해 주세요.");

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
