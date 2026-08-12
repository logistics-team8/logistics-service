package com.logistics.userservice.presentation;

import com.logistics.common.response.ApiResponse;
import com.logistics.userservice.application.UserService;
import com.logistics.userservice.presentation.dto.internal.InternalUserInfoResponse;
import com.logistics.userservice.presentation.dto.internal.InternalUserRoleResponse;
import com.logistics.userservice.presentation.dto.internal.InternalUserSlackResponse;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/users")
public class InternalUserController {
    private final UserService userService;

    @GetMapping("/{userId}")
    public ApiResponse<InternalUserInfoResponse> getUserInfo(@PathVariable UUID userId) {
        return ApiResponse.success(InternalUserInfoResponse.from(userService.getUserInfo(userId)));
    }

    @GetMapping("/{userId}/role")
    public ApiResponse<InternalUserRoleResponse> getUserRole(@PathVariable UUID userId) {
        return ApiResponse.success(InternalUserRoleResponse.from(userService.getUserRole(userId)));
    }

    @GetMapping("/{userId}/slack")
    public ApiResponse<InternalUserSlackResponse> getUserSlackId(@PathVariable UUID userId) {
        return ApiResponse.success(
                InternalUserSlackResponse.from(userService.getUserSlackId(userId)));
    }
}
