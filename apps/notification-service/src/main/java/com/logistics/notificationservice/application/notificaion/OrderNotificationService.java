package com.logistics.notificationservice.application.notificaion;

import com.logistics.notificationservice.application.ai.*;
import com.logistics.notificationservice.application.ai.AiDispatchCommand;
import com.logistics.notificationservice.application.slack.SlackClient;
import com.logistics.notificationservice.presentation.slack.dto.OrderNotificationRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderNotificationService {

    private final ApplicationEventPublisher eventPublisher;

    public void notifyOrderCreated(
            OrderNotificationRequestDto request
    ) {
        eventPublisher.publishEvent(
                new OrderNotificationEvent(request)
        );
    }
}