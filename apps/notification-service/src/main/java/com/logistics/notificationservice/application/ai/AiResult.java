package com.logistics.notificationservice.application.ai;

import java.time.LocalDateTime;

public record AiResult (
        String rawResponse,
        LocalDateTime finalDispatchDeadline,
        String modelName,
        Integer responseTimeMs
){




}
