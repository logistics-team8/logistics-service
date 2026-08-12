package com.logistics.notificationservice.application.notificaion;

import com.logistics.notificationservice.presentation.slack.dto.DispatchNotificationRequestDto;

public record OrderNotificationEvent(DispatchNotificationRequestDto request) {
}