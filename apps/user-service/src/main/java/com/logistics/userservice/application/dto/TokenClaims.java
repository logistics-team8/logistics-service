package com.logistics.userservice.application.dto;

import com.logistics.userservice.domain.Role;
import java.util.UUID;

public record TokenClaims(UUID userId, UUID hubId, UUID companyId, Role role) {}
