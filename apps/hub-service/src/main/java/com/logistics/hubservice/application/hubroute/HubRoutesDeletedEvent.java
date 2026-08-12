package com.logistics.hubservice.application.hubroute;

import java.util.List;
import java.util.UUID;

public record HubRoutesDeletedEvent(List<UUID> hubRouteIds) {

    public HubRoutesDeletedEvent {
        hubRouteIds = List.copyOf(hubRouteIds);
    }
}
