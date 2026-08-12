package com.logistics.deliveryservice.domain.port;

import java.util.UUID;

// Hub Service에 이 허브가 존재하고 활성 상태인지 검증하는 인터페이스
public interface DeliveryManagerHubValidator {

    void validateActiveHub(UUID hubId);
}
