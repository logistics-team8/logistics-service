package com.logistics.userservice.application.dto.admin;

import com.logistics.userservice.domain.Role;
import com.logistics.userservice.domain.User;
import com.logistics.userservice.domain.UserStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record AdminUserInfo(
        UUID userId,
        String username,
        String name,
        UUID hubId,
        UUID companyId,
        Role role,
        UserStatus userStatus,
        UUID approved_by,
        LocalDateTime approved_at,
        String rejectionReason,
        LocalDateTime createdAt) {
    public static AdminUserInfo from(User user) {
        return new AdminUserInfo(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getHubId(),
                user.getCompanyId(),
                user.getRole(),
                user.getUserStatus(),
                user.getApprovedBy(),
                user.getApprovedAt(),
                user.getRejectionReason(),
                user.getCreatedAt());
    }
}
