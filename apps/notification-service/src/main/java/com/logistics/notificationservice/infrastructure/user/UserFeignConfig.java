package com.logistics.notificationservice.infrastructure.user;

import com.logistics.notificationservice.infrastructure.security.ServiceTokenProvider;
import feign.RequestInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class UserFeignConfig {

    private final ServiceTokenProvider serviceTokenProvider;

    @Bean
    public RequestInterceptor userServiceTokenInterceptor(){

        return requestTemplate -> {

            String token = serviceTokenProvider.createToken("notification-service");

            requestTemplate.header("Authorization", "Bearer " + token);
        };
    }
}
