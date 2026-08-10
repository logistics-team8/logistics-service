package com.logistics.userservice.application.token;

import java.util.UUID;

public record TokenClaims(UUID userId, UUID hubId, UUID companyId) {}
