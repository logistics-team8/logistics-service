package com.logistics.deliveryservice.domain.port;

import java.util.UUID;

public interface UserSlackProvider {

    String getSlackId(UUID userId);
}
