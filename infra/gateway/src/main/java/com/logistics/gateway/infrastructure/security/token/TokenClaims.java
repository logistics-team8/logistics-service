package com.logistics.gateway.infrastructure.security.token;

import java.util.UUID;

public record TokenClaims(UUID userId, UUID hubId, UUID companyId) {}
