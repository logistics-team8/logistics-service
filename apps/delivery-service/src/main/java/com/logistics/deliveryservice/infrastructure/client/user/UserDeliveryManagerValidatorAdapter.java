package com.logistics.deliveryservice.infrastructure.client.user;

import com.logistics.deliveryservice.domain.exception.DeliveryErrorCode;
import com.logistics.deliveryservice.domain.exception.DeliveryException;
import com.logistics.deliveryservice.domain.port.DeliveryManagerUserValidator;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class UserDeliveryManagerValidatorAdapter implements DeliveryManagerUserValidator {

    private final UserRoleFeignClient userRoleFeignClient;

    @Override
    public void validateDeliveryManager(UUID userId) {

        // FeignClient를 통해 UserRole(managerType)을 조회
        UserRoleFeignClient.UserRoleResponse response = userRoleFeignClient.getUserRole(userId);

        // 값이 없거나 DELIVERY_MANAGER가 아닌 경우 DeliveryException
        if (response == null || !"DELIVERY_MANAGER".equals(response.role())) {
            throw new DeliveryException(DeliveryErrorCode.INVALID_DELIVERY_MANAGER_USER);
        }
    }
}
