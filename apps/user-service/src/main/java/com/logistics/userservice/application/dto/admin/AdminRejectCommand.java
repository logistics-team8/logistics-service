package com.logistics.userservice.application.dto.admin;

import com.logistics.userservice.domain.Role;
import java.util.UUID;

public record AdminRejectCommand(UUID adminId, Role role, UUID hubId, UUID userId, String reason) {}
