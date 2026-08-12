package com.logistics.userservice.error;

import com.logistics.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {
    INVALID_LOGIN("AUTH_001", HttpStatus.UNAUTHORIZED, "아이디가 존재하지 않거나 비밀번호가 올바르지 않습니다."),

    TOKEN_EXPIRED("AUTH_101", HttpStatus.UNAUTHORIZED, "인증 정보가 만료되었습니다."),
    TOKEN_INVALID("AUTH_102", HttpStatus.UNAUTHORIZED, "유효하지 않은 인증 정보입니다."),
    PENDING_APPROVAL("AUTH_103", HttpStatus.FORBIDDEN, "승인 대기중인 계정입니다."),
    APPROVAL_REJECTED("AUTH_104", HttpStatus.FORBIDDEN, "승인 거절된 계정입니다."),
    HUB_ID_REQUIRED("AUTH_105", HttpStatus.BAD_REQUEST, "허브 소속 회원은 허브 ID를 입력해야 합니다."),
    COMPANY_ID_REQUIRED("AUTH_106", HttpStatus.BAD_REQUEST, "업체 소속 회원은 업체 ID를 입력해야 합니다"),
    MASTER_ROLE_NOT_ALLOWED("AUTH_107", HttpStatus.BAD_REQUEST, "마스터 권한은 선택할 수 없습니다.");

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
