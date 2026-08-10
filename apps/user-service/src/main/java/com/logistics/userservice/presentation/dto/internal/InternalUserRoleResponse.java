package com.logistics.userservice.presentation.dto.internal;

import com.logistics.userservice.application.dto.user.UserRoleInfo;
import com.logistics.userservice.domain.Role;

public record InternalUserRoleResponse(Role role) {
    public static InternalUserRoleResponse from(UserRoleInfo userRoleInfo) {
        return new InternalUserRoleResponse(userRoleInfo.role());
    }
}
