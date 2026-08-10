package com.logistics.userservice.infrastructure.client.delivery;

import com.logistics.common.error.CommonErrorCode;
import com.logistics.common.exception.BusinessException;
import com.logistics.userservice.application.port.DeliveryClientPort;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryFeignAdapter implements DeliveryClientPort {
    private final DeliveryFeignClient deliveryFeignClient;

    @Override
    public void createDeliveryManager(UUID userId, DeliveryManagerType managedType, UUID hubId) {
        try {
            CreateDeliveryManagerRequest request = CreateDeliveryManagerRequest.of(userId, managedType, hubId);
            deliveryFeignClient.createDeliveryManager(request);
            log.info("[SUCCESS] DeliveryService 배송 관리자 생성 성공 userId = {}", userId);
        } catch (FeignException e) {
            log.error("[ERROR] DeliveryService 호출 실패 userId = {}", userId, e);
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
