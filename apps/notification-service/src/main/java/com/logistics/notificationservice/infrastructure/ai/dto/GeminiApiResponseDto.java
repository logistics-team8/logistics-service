package com.logistics.notificationservice.infrastructure.ai.dto;

import java.util.List;

public record GeminiApiResponseDto(
        List<Candidate> candidates,
        UsageMetadata usageMetadata
){
    public String getResponseText() {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        Candidate candidate = candidates.get(0);

        if (candidate.content() == null
                || candidate.content().parts() == null
                || candidate.content().parts().isEmpty()) {
            return null;
        }

        return candidate.content()
                .parts()
                .get(0)
                .text();
    }

    public record Candidate(
            Content content,
            String finishReason
    ) {
    }

    public record Content(
            List<Part> parts
    ) {
    }

    public record Part(
            String text
    ) {
    }

    public record UsageMetadata(
            Integer promptTokenCount,
            Integer candidatesTokenCount,
            Integer totalTokenCount
    ) {
    }


}
