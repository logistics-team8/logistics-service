package com.logistics.gateway.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "path")
public record PathProperties(List<PathPattern> whitelist) {
    public record PathPattern(String method, String pattern) {}
}
