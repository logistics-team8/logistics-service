package com.logistics.deliveryservice.infrastructure.client.user;

import com.logistics.common.response.ApiResponse;
import com.logistics.deliveryservice.infrastructure.client.user.dto.UserSlackResponse;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service")
public interface UserSlackFeignClient {

    @GetMapping("/internal/v1/users/{userId}/slack")
    ApiResponse<UserSlackResponse> getUserSlackId(@PathVariable UUID userId);
}
