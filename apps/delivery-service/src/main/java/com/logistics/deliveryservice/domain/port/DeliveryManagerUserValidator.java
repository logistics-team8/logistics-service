package com.logistics.deliveryservice.domain.port;

import java.util.UUID;

// User Service에 이 사용자가 배송 담당자인지 검증하는 인터페이스
public interface DeliveryManagerUserValidator {

    void validateDeliveryManager(UUID userId);
}
