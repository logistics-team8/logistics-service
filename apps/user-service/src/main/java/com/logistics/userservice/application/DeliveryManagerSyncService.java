package com.logistics.userservice.application;

import com.logistics.common.exception.BusinessException;
import com.logistics.userservice.application.dto.delivery.DeliveryManagerCreateCommand;
import com.logistics.userservice.application.port.DeliveryClientPort;
import com.logistics.userservice.domain.User;
import com.logistics.userservice.domain.UserRepository;
import com.logistics.userservice.domain.UserStatus;
import com.logistics.userservice.error.UserErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryManagerSyncService {
    private final DeliveryClientPort deliveryClientPort;
    private final UserRepository userRepository;

    /**
     * 배송 담당자 승인
     *
     * @param command 승인할 회원 정보
     */
    @Transactional
    public void create(DeliveryManagerCreateCommand command) {
        User user =
                userRepository
                        .findByIdAndDeletedAtIsNull(command.userId())
                        .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        if (user.getUserStatus() != UserStatus.PROCESSING) {
            log.info("[SUCCESS] 배송 담당자 상태 동기화 불필요");
            return;
        }

        // Delivery Service 호출
        deliveryClientPort.createDeliveryManager(
                command.userId(), command.hubId(), command.managerType());

        user.completeProvisioning();
    }

    /** 배송 담당자 PROCESSING 상태 동기화 */
    @Transactional
    public void syncProcessingDeliveryManagers() {
        List<User> processingUsers =
                userRepository.findAllByUserStatus(UserStatus.PROCESSING, PageRequest.of(0, 100));

        if (processingUsers.isEmpty()) {
            log.info("[SUCCESS] 배송 담당자 상태 동기화 불필요");
            return;
        }

        log.info("[START] 배송 담당자 PROCESSING 상태 동기화 시작 {} 건", processingUsers.size());
        for (User user : processingUsers) {
            try {
                var command =
                        DeliveryManagerCreateCommand.of(
                                user.getId(), user.getHubId(), user.getRequestedRole());

                deliveryClientPort.createDeliveryManager(
                        command.userId(), command.hubId(), command.managerType());

                user.completeProvisioning();
            } catch (Exception e) {
                log.error("[ERRER] 배송 담당자 동기화 오류 userId = {}", user.getId(), e);
            }
        }
        log.info("[END] 배송 담당자 PROCESSING 상태 동기화 종료");
    }
}
