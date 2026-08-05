package com.logistics.gateway.infrastructure.security.token;

import com.logistics.gateway.domain.Role;

import java.util.UUID;

public record TokenClaims(UUID userId, UUID hubId, UUID companyId, Role role) {}
