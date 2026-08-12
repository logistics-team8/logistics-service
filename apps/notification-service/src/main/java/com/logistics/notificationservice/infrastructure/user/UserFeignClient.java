package com.logistics.notificationservice.infrastructure.user;

import com.logistics.notificationservice.infrastructure.user.dto.InternalUserSlackResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "user-service")
public interface  UserFeignClient {

    @GetMapping("/internal/v1/users/{userId}/slack")
    InternalUserSlackResponseDto getSlackId(UUID userId);
}
