package com.logistics.gateway.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "path")
public record PathProperties(List<String> whitelist) {}
