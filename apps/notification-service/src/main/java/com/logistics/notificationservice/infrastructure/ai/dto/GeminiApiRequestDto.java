package com.logistics.notificationservice.infrastructure.ai.dto;

import java.util.List;
import java.util.Map;

public record GeminiApiRequestDto(
        List<Content> contents,
        GenerationConfig generationConfig
) {

    public static GeminiApiRequestDto from(String prompt) {

        return new GeminiApiRequestDto(
                List.of(
                        new Content(
                                List.of(
                                        new Part(prompt)
                                )
                        )
                ),
                new GenerationConfig(
                        "application/json",
                        Map.of(
                                "type", "OBJECT",
                                "properties", Map.of(
                                        "finalDispatchDeadline", Map.of(
                                                "type", "STRING"
                                        )
                                ),
                                "required", List.of(
                                        "finalDispatchDeadline"
                                )
                        )
                )
        );
    }

    public record Content(
            List<Part> parts
    ) {
    }

    public record Part(
            String text
    ) {
    }

    public record GenerationConfig(
            String responseMimeType,
            Map<String, Object> responseSchema
    ) {
    }
}