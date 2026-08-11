package com.logistics.userservice.application.dto.user;

import com.logistics.userservice.domain.RequestedRole;
import java.util.UUID;

public record UserCreateCommand(
        String username,
        String password,
        String name,
        String slackId,
        UUID hubId,
        UUID companyId,
        RequestedRole requestedRole,
        AffiliationType affiliationType) {}
