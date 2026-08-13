package com.logistics.notificationservice.presentation.slack.dto;

import com.logistics.notificationservice.domain.slack.SlackMessage;
import com.logistics.notificationservice.domain.slack.SlackMessageStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record SlackMessageResponseDto(
        UUID slackMessageId,
        UUID orderId,
        UUID aiRequestId,

        UUID recipientUserId,
        String recipientSlackId,

        String message,
        SlackMessageStatus status,

        Integer retryCount,
        String failureReason,

        LocalDateTime sentAt
    ){
    public static SlackMessageResponseDto from(
            SlackMessage slackMessage
    ) {

        return new SlackMessageResponseDto(
                slackMessage.getSlackMessageId(),
                slackMessage.getOrderId(),
                slackMessage.getAiRequestId(),
                slackMessage.getRecipientUserId(),
                slackMessage.getRecipientSlackId(),
                slackMessage.getMessage(),
                slackMessage.getStatus(),
                slackMessage.getRetryCount(),
                slackMessage.getFailureReason(),
                slackMessage.getSentAt()
        );
    }
}