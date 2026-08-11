package com.logistics.notificationservice.infrastructure.slack;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.http.HttpHeaders;



@Configuration
@EnableConfigurationProperties(SlackProperties.class)
public class SlackConfig {

    @Bean("slackRestClient")
    public RestClient slackRestClient(
            SlackProperties properties
    ) {
        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + properties.getBotToken()
                        )
                .defaultHeader(
                        HttpHeaders.CONTENT_TYPE,
                        MediaType.APPLICATION_JSON_VALUE
                ).build();

    }


}
