package com.logistics.userservice.infrastructure.client.delivery;

import com.logistics.common.exception.BusinessException;
import com.logistics.userservice.application.port.DeliveryClientPort;
import com.logistics.userservice.domain.RequestedRole;
import com.logistics.userservice.error.ClientErrorCode;
import feign.FeignException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryFeignAdapter implements DeliveryClientPort {
    private final DeliveryFeignClient deliveryFeignClient;

    @Override
    public void createDeliveryManager(UUID userId, UUID hubId, RequestedRole managerType) {
        try {
            CreateDeliveryManagerRequest request =
                    CreateDeliveryManagerRequest.of(userId, hubId, managerType);

            deliveryFeignClient.createDeliveryManager(request);
            log.info("[SUCCESS] 배송 관리자 생성 성공 userId = {}", userId);

        } catch (FeignException.Conflict e) {
            log.info("[SUCCESS] 이미 생성된 배송 관리자 userId = {}", userId);

        } catch (FeignException e) {
            log.error(
                    "[ERROR] Delivery-Service 호출 실패 userId = {}, status = {}, content = {}",
                    userId,
                    e.status(),
                    e.contentUTF8(),
                    e);
            throw new BusinessException(ClientErrorCode.SERVICE_UNAVAILABLE);
        }
    }
}
