package com.logistics.userservice.application.dto;

import com.logistics.userservice.domain.Role;
import java.util.UUID;

public record UserRole(UUID userId, Role role) {}
