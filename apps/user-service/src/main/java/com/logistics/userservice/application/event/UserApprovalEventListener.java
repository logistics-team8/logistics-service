package com.logistics.userservice.application.event;

import com.logistics.userservice.application.DeliveryManagerService;
import com.logistics.userservice.application.dto.delivery.DeliveryManagerCreateCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserApprovalEventListener {
    private final DeliveryManagerService deliveryManagerService;

    @Async
    @TransactionalEventListener
    public void handleUserApproveEvent(UserApprovalEvent event) {
        deliveryManagerService.create(
                DeliveryManagerCreateCommand.of(
                        event.userId(), event.hubId(), event.managerType()));
    }
}
