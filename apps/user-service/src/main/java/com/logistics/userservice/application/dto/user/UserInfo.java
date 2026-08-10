package com.logistics.userservice.application.dto.user;

import com.logistics.userservice.domain.Role;
import com.logistics.userservice.domain.User;
import java.time.LocalDateTime;
import java.util.UUID;

public record UserInfo(
        UUID userId,
        String username,
        String name,
        String slackId,
        UUID hubId,
        UUID companyId,
        Role role,
        LocalDateTime createdAt) {
    public static UserInfo from(User user) {
        return new UserInfo(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getSlackId(),
                user.getHubId(),
                user.getCompanyId(),
                user.getRole(),
                user.getCreatedAt());
    }
}
