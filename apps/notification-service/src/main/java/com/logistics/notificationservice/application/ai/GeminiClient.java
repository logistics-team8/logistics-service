package com.logistics.notificationservice.application.ai;

import com.logistics.notificationservice.infrastructure.ai.dto.AiDispatchResultDto;

public interface GeminiClient {

    GeminiResult generateDispatchDeadline(String prompt);

    record GeminiResult(
            AiDispatchResultDto result,
            String rawResponse,
            String modelName,
            Long responseTimeMs
    ) {
    }
}

