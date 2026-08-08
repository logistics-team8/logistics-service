package com.logistics.notificationservice.presentation.slack.dto;

import jakarta.validation.constraints.NotBlank;

public record SlackMessageRequestDto (
        @NotBlank(message = "메시지는 필수입니다.")
        String text
){
}
