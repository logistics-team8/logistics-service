package com.logistics.deliveryservice.infrastructure.client.user;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// User Service에서 논리 삭제 되지 않은 사용자의 역할을 조회
@FeignClient(name = "user-service")
public interface UserRoleFeignClient {

    @GetMapping("/internal/v1/users/{userId}/role")
    UserRoleResponse getUserRole(@PathVariable UUID userId);

    record UserRoleResponse(String role) {
    }
}
