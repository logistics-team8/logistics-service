package com.logistics.userservice.presentation.dto.admin;

import com.logistics.userservice.application.dto.admin.UserApprovalInfo;
import com.logistics.userservice.domain.Role;
import com.logistics.userservice.domain.UserStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record UserApprovalInfoResponse(
        UUID userId,
        String username,
        String name,
        UUID hubId,
        UUID companyId,
        Role role,
        UserStatus userStatus,
        String rejectionReason,
        LocalDateTime createdAt) {
    public static UserApprovalInfoResponse from(UserApprovalInfo userInfo) {
        return new UserApprovalInfoResponse(
                userInfo.userId(),
                userInfo.username(),
                userInfo.name(),
                userInfo.hubId(),
                userInfo.companyId(),
                userInfo.role(),
                userInfo.userStatus(),
                userInfo.rejectionReason(),
                userInfo.createdAt());
    }
}
