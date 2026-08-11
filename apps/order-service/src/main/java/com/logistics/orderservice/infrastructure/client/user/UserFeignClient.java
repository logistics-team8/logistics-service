package com.logistics.orderservice.infrastructure.client.user;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(
        name = "user-service",
        path = "/internal/v1/users"
)
public interface UserFeignClient {

    @GetMapping("/{userId}")
    UserResponse getUser(@PathVariable("userId")UUID userId);

    record UserResponse(
            UUID userId,
            String username,
            String name,
            String slackId,
            UUID hubId,
            UUID companyId,
            String role
    ){
    }
}
