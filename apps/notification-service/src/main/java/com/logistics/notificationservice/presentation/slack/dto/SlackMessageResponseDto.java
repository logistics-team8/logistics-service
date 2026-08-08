package com.logistics.notificationservice.presentation.slack.dto;

public record SlackMessageResponseDto(
    String channelId,
    String messageTimestamp,
    String text
    ){ }
