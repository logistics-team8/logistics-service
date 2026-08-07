package com.logistics.gateway.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "path")
public record PathProperties(List<String> whitelist) {}
