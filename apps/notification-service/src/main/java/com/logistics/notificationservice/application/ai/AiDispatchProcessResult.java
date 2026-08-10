package com.logistics.notificationservice.application.ai;

import com.logistics.notificationservice.infrastructure.ai.dto.AiDispatchResultDto;

import java.util.UUID;

public record AiDispatchProcessResult(
        UUID aiRequestId,
        AiDispatchResultDto result
) {
}