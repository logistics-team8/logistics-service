package com.logistics.userservice.application.port;

import java.util.UUID;

public interface HubClientPort {
    boolean existsById(UUID hubId);
}
