package com.logistics.userservice.domain;

import com.logistics.common.exception.BusinessException;
import com.logistics.userservice.error.AuthErrorCode;

public enum UserStatus {
    PENDING, // 승인 대기
    PROCESSING, // 외부 서비스 연동 실패 시 상태 값
    APPROVED, // 승인 완료
    REJECTED; // 승인 거절

    public void validateStatus() {
        switch (this) {
            case APPROVED -> throw new BusinessException(AuthErrorCode.ALREADY_APPROVED);
            case REJECTED -> throw new BusinessException(AuthErrorCode.ALREADY_REJECTED);
            case PROCESSING -> throw new BusinessException(AuthErrorCode.ALREADY_PROCESSING);
        }
    }
}
