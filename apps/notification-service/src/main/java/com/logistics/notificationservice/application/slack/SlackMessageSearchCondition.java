package com.logistics.notificationservice.application.slack;

import com.logistics.notificationservice.domain.slack.SlackMessageStatus;

import java.util.UUID;

public record SlackMessageSearchCondition(
        SlackMessageStatus status,
        UUID orderId,
        UUID recipientUserId,
        String recipientSlackId,
        String keyword

) {
}
