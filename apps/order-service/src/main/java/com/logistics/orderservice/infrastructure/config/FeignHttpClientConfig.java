package com.logistics.orderservice.infrastructure.config;

import feign.Retryer;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 재시도 횟수와 상태 확인 순서를 Order 애플리케이션에서 통제한다.
 */
@Configuration
public class FeignHttpClientConfig {

    @Bean
    public CloseableHttpClient feignHttpClient() {
        return HttpClients.custom()
                .disableAutomaticRetries()
                .build();
    }

    @Bean
    public Retryer feignRetryer() {
        return Retryer.NEVER_RETRY;
    }
}
