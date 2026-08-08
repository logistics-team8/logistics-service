package com.logistics.userservice.presentation.dto.user;

import com.logistics.userservice.application.dto.UserInfo;
import com.logistics.userservice.domain.Role;
import java.time.LocalDateTime;
import java.util.UUID;

public record UserInfoResponse(
        String username,
        String name,
        String slackId,
        UUID hubId,
        UUID companyId,
        Role role,
        LocalDateTime createdAt) {
    public static UserInfoResponse from(UserInfo userInfo) {
        return new UserInfoResponse(
                userInfo.username(),
                userInfo.name(),
                userInfo.slackId(),
                userInfo.hubId(),
                userInfo.companyId(),
                userInfo.role(),
                userInfo.createdAt());
    }
}
