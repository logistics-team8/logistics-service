package com.logistics.userservice.application.dto.admin;

import com.logistics.userservice.domain.Role;
import com.logistics.userservice.domain.User;
import com.logistics.userservice.domain.UserStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record UserApprovalInfo(
        UUID userId,
        String username,
        String name,
        UUID hubId,
        UUID companyId,
        Role role,
        UserStatus userStatus,
        String rejectionReason,
        LocalDateTime createdAt) {
    public static UserApprovalInfo from(User user) {
        return new UserApprovalInfo(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getHubId(),
                user.getCompanyId(),
                user.getRole(),
                user.getUserStatus(),
                user.getRejectionReason(),
                user.getCreatedAt());
    }
}
