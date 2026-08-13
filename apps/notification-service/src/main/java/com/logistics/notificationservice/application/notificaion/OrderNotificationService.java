package com.logistics.notificationservice.application.notificaion;

import com.logistics.notificationservice.presentation.slack.dto.DispatchNotificationRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderNotificationService {

    private final ApplicationEventPublisher eventPublisher;

    public void notifyOrderCreated(DispatchNotificationRequestDto request) {
        eventPublisher.publishEvent(
                new OrderNotificationEvent(request)
        );
    }
}