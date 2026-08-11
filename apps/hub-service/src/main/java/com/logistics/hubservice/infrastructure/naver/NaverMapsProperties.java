package com.logistics.hubservice.infrastructure.naver;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "naver.maps")
public record NaverMapsProperties(
        @NotBlank String baseUrl,
        @NotBlank String apiKeyId,
        @NotBlank String apiKey) {
}
