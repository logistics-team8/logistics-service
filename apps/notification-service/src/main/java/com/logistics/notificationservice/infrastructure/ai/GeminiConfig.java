package com.logistics.notificationservice.infrastructure.ai;


import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(GeminiProperties.class)
public class GeminiConfig {

    @Bean("geminiRestClient")
    public RestClient geminiRestClient(
            GeminiProperties properties
    ){
        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(
                        "x-goog-api-key",
                        properties.getApiKey()
                )
                .defaultHeader(
                        "Content-Type",
                        MediaType.APPLICATION_JSON_VALUE
                ).build();
    }
}
