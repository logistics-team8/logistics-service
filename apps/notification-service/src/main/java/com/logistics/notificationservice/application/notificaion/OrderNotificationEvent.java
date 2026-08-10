package com.logistics.notificationservice.application.notificaion;

import com.logistics.notificationservice.presentation.slack.dto.OrderNotificationRequestDto;

public record OrderNotificationEvent(
        OrderNotificationRequestDto request
) {
}