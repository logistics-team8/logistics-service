package com.logistics.userservice.application.token;

import java.util.UUID;

public record TokenPayload(UUID userId, UUID sessionId, UUID hubId, UUID companyId) {}
