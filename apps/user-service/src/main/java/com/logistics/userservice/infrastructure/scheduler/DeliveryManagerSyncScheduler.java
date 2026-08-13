package com.logistics.userservice.infrastructure.scheduler;

import com.logistics.userservice.application.DeliveryManagerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryManagerSyncScheduler {
    private final DeliveryManagerService deliveryManagerService;

    @Scheduled(cron = "0 */5 * * * *")
    public void execute() {
        log.info("[START] 배송 담당자 동기화 스케쥴러 실행");
        deliveryManagerService.syncProcessingDeliveryManagers();
        log.info("[END] 배송 담당자 동기화 스케쥴러 종료");
    }
}
