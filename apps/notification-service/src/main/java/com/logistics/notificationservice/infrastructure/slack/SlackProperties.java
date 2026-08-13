package com.logistics.notificationservice.infrastructure.slack;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "slack")
public class SlackProperties {
    private String botToken;
    private String baseUrl;
}
