package com.logistics.userservice.application.event;

import com.logistics.userservice.domain.RequestedRole;
import java.util.UUID;

public record UserApprovalEvent(UUID userId, UUID hubId, RequestedRole managerType) {}
