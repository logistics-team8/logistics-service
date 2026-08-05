package com.logistics.gateway.config;

import com.logistics.gateway.presentation.error.GatewayErrorCode;
import com.logistics.gateway.presentation.exception.BusinessException;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@TestConfiguration
public class TestFilterConfig {
    @Bean
    GlobalFilter testFilter() {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getPath().value();

            if (path.equals("/business-exception")) {
                return Mono.error(new BusinessException(GatewayErrorCode.TOKEN_EXPIRED));
            }

            if (path.equals("/not-found")) {
                return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND));
            }

            if (path.equals("/bad-request")) {
                return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST));
            }

            if (path.equals("/internal-server-error")) {
                return Mono.error(new IllegalStateException("test"));
            }

            return chain.filter(exchange);
        };
    }
}
