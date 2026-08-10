package com.logistics.notificationservice.infrastructure.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.notificationservice.application.ai.GeminiClient;
import com.logistics.notificationservice.domain.common.exception.NotificationErrorCode;
import com.logistics.notificationservice.domain.common.exception.NotificationException;
import com.logistics.notificationservice.infrastructure.ai.dto.AiDispatchResultDto;
import com.logistics.notificationservice.infrastructure.ai.dto.GeminiApiRequestDto;
import com.logistics.notificationservice.infrastructure.ai.dto.GeminiApiResponseDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GeminiClientImpl implements GeminiClient {

    private final RestClient geminiRestClient;
    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper;

    public GeminiClientImpl(
            @Qualifier("geminiRestClient") RestClient geminiRestClient,
            GeminiProperties geminiProperties,
            ObjectMapper objectMapper
    ) {
        this.geminiRestClient = geminiRestClient;
        this.geminiProperties = geminiProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public GeminiResult generateDispatchDeadline(String prompt) {

        long startTime = System.currentTimeMillis();

        GeminiApiRequestDto request = GeminiApiRequestDto.from(prompt);

        GeminiApiResponseDto response =
                geminiRestClient
                        .post()
                        .uri(uriBuilder -> uriBuilder
                                .path("/v1beta/models/{model}:generateContent")
                                .build(geminiProperties.getModel())
                        )
                        .body(request)
                        .retrieve()
                        .body(GeminiApiResponseDto.class);

        long responseTimeMs =
                System.currentTimeMillis() - startTime;

        if (response == null) {
            throw new NotificationException(
                    NotificationErrorCode.GEMINI_RESPONSE_EMPTY
            );
        }

        String responseText = response.getResponseText();

        if (responseText == null || responseText.isBlank()) {
            throw new NotificationException(
                    NotificationErrorCode.GEMINI_RESPONSE_CONTENT_EMPTY
            );
        }

        try {
            AiDispatchResultDto result =
                    objectMapper.readValue(
                            responseText,
                            AiDispatchResultDto.class
                    );

            return new GeminiResult(
                    result,
                    responseText,
                    geminiProperties.getModel(),
                    responseTimeMs
            );

        } catch (JsonProcessingException e) {
            throw new NotificationException(
                    NotificationErrorCode.GEMINI_RESPONSE_PARSE_FAILED,
                    e
            );
        }
    }
}