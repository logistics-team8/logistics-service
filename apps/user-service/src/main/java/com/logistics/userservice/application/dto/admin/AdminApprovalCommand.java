package com.logistics.userservice.application.dto.admin;

import com.logistics.common.security.principal.CustomUserDetails;
import com.logistics.userservice.domain.Role;
import java.util.UUID;

public record AdminApprovalCommand(UUID adminId, Role role, UUID hubId, UUID userId) {
    public static AdminApprovalCommand of(CustomUserDetails principal, UUID userId) {
        return new AdminApprovalCommand(
                principal.getId(), Role.valueOf(principal.getRole()), principal.getHubId(), userId);
    }
}
