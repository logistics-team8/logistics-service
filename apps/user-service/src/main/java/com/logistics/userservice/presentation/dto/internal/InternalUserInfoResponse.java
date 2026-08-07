package com.logistics.userservice.presentation.dto.internal;

import com.logistics.userservice.application.dto.UserInfo;
import com.logistics.userservice.domain.Role;
import java.time.LocalDateTime;
import java.util.UUID;

public record InternalUserInfoResponse(
        UUID userId,
        String username,
        String name,
        String slackId,
        UUID hubId,
        UUID companyId,
        Role role,
        LocalDateTime createdAt) {
    public static InternalUserInfoResponse from(UserInfo userInfo) {
        return new InternalUserInfoResponse(
                userInfo.userId(),
                userInfo.username(),
                userInfo.name(),
                userInfo.slackId(),
                userInfo.hubId(),
                userInfo.companyId(),
                userInfo.role(),
                userInfo.createdAt());
    }
}
