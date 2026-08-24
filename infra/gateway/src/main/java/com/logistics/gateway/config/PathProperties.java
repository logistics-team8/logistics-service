package com.logistics.gateway.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpMethod;

@ConfigurationProperties(prefix = "path")
public record PathProperties(List<PathPattern> whitelist) {
    public record PathPattern(HttpMethod method, String path) {}
}
