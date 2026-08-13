package com.logistics.userservice.application.dto.admin;

import com.logistics.userservice.domain.Role;
import com.logistics.userservice.domain.UserStatus;
import java.util.UUID;

public record SearchUsersQuery(
        String username,
        String name,
        UUID hubId,
        UUID companyId,
        Role role,
        UserStatus userStatus) {
    public SearchUsersQuery {
        userStatus = userStatus == null ? UserStatus.APPROVED : userStatus;
    }
}
