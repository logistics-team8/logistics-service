package com.logistics.userservice.presentation.dto.admin;

import com.logistics.userservice.application.dto.admin.AdminUserInfo;
import com.logistics.userservice.domain.Role;
import com.logistics.userservice.domain.UserStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record AdminUserInfoResponse(
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
    public static AdminUserInfoResponse from(AdminUserInfo userInfo) {
        return new AdminUserInfoResponse(
                userInfo.userId(),
                userInfo.username(),
                userInfo.name(),
                userInfo.hubId(),
                userInfo.companyId(),
                userInfo.role(),
                userInfo.userStatus(),
                userInfo.approved_by(),
                userInfo.approved_at(),
                userInfo.rejectionReason(),
                userInfo.createdAt());
    }
}
