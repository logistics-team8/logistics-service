package com.logistics.userservice.application.dto.user;

import com.logistics.common.security.principal.CustomUserDetails;
import com.logistics.userservice.domain.Role;
import java.util.UUID;

public record UserContext(UUID userId, Role role, UUID hubId) {
    public static UserContext from(CustomUserDetails principal) {
        return new UserContext(
                principal.getId(), Role.valueOf(principal.getRole()), principal.getHubId());
    }

    public boolean isHubManager() {
        return role == Role.HUB_MANAGER;
    }
}
