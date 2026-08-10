package com.logistics.userservice.application.dto.user;

import com.logistics.userservice.domain.Role;
import java.util.UUID;

public record UserCreateCommand(
        String username,
        String password,
        String name,
        String slackId,
        UUID hub_id,
        UUID company_id,
        Role role) {}
