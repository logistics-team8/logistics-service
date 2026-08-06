package com.logistics.notificationservice.infrastructure.ai;

import com.logistics.notificationservice.application.ai.AiClient;
import com.logistics.notificationservice.application.ai.AiResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GeminiClient implements AiClient {
    @Override
    public AiResult calculateDispatchDeadline(String prompt) {
        return null;
    }
}
