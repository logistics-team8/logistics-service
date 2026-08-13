package com.logistics.notificationservice.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.logistics.notificationservice.infrastructure.ai.GeminiConfig;
import com.logistics.notificationservice.infrastructure.ai.GeminiProperties;
import com.logistics.notificationservice.infrastructure.slack.SlackConfig;
import com.logistics.notificationservice.infrastructure.slack.SlackProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class RestClientConfigTest {

    @Test
    void slackClientUsesInjectedBuilder() {
        SlackProperties properties = new SlackProperties();
        properties.setBaseUrl("https://slack.example");
        properties.setBotToken("test-token");
        RestClient.Builder builder = spy(RestClient.builder());

        RestClient restClient = new SlackConfig().slackRestClient(properties, builder);

        assertThat(restClient).isNotNull();
        verify(builder).baseUrl(properties.getBaseUrl());
    }

    @Test
    void geminiClientUsesInjectedBuilder() {
        GeminiProperties properties = new GeminiProperties();
        properties.setBaseUrl("https://gemini.example");
        properties.setApiKey("test-key");
        RestClient.Builder builder = spy(RestClient.builder());

        RestClient restClient = new GeminiConfig().geminiRestClient(properties, builder);

        assertThat(restClient).isNotNull();
        verify(builder).baseUrl(properties.getBaseUrl());
    }
}
