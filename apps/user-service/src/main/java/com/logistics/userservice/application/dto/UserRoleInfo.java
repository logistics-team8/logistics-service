package com.logistics.userservice.application.dto;

import com.logistics.userservice.domain.Role;
import java.util.UUID;

public record UserRoleInfo(UUID userId, Role role) {}
