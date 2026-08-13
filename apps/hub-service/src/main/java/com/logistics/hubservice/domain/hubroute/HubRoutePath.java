package com.logistics.hubservice.domain.hubroute;

import java.util.List;

public record HubRoutePath(
        List<HubRoute> segments,
        long totalDistanceMeters,
        long totalDurationSeconds
) {

    public HubRoutePath {
        segments = List.copyOf(segments);
    }

    public static HubRoutePath empty() {
        return new HubRoutePath(List.of(), 0L, 0L);
    }
}
