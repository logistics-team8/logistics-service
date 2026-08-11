package com.logistics.userservice.domain;

public enum UserStatus {
    PENDING, // 승인 대기
    PROCESSING, // 외부 서비스 연동 실패 시 상태 값
    APPROVED, // 승인 완료
    REJECTED, // 승인 거절
}
