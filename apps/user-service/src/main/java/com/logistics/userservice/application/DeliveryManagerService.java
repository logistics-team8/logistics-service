package com.logistics.userservice.application;

import com.logistics.common.exception.BusinessException;
import com.logistics.userservice.application.dto.delivery.DeliveryManagerCreateCommand;
import com.logistics.userservice.application.port.DeliveryClientPort;
import com.logistics.userservice.domain.User;
import com.logistics.userservice.domain.UserRepository;
import com.logistics.userservice.domain.UserStatus;
import com.logistics.userservice.error.UserErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeliveryManagerService {
    private final DeliveryClientPort deliveryClientPort;
    private final UserRepository userRepository;

    /**
     * 배송 담당자 승인
     *
     * @param command
     */
    @Transactional
    public void create(DeliveryManagerCreateCommand command) {
        User user =
                userRepository
                        .findByIdAndDeletedAtIsNull(command.userId())
                        .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        if (user.getUserStatus() != UserStatus.PROVISIONING) {
            return;
        }

        // Delivery Service 호출
        deliveryClientPort.createDeliveryManager(
                command.userId(), command.hubId(), command.managerType());

        user.completeProvisioning();
    }
}
