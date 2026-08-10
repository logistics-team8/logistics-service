package com.logistics.userservice.infrastructure.client.hub;

import java.util.UUID;

public record HubExistsResponse(
        UUID hubId,
        boolean exists
) {}
