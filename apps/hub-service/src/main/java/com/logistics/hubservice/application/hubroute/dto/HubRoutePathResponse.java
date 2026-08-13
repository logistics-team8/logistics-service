package com.logistics.hubservice.application.hubroute.dto;

import com.logistics.hubservice.domain.hubroute.HubRoute;
import com.logistics.hubservice.domain.hubroute.HubRoutePath;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record HubRoutePathResponse(
        UUID sourceHubId,
        UUID destinationHubId,
        long totalDistanceMeters,
        long totalDurationSeconds,
        List<Segment> segments
) {

    public HubRoutePathResponse {
        segments = List.copyOf(segments);
    }

    public static HubRoutePathResponse from(
            UUID sourceHubId,
            UUID destinationHubId,
            HubRoutePath path) {
        List<Segment> segments = new ArrayList<>();
        for (int index = 0; index < path.segments().size(); index++) {
            segments.add(Segment.from(index + 1, path.segments().get(index)));
        }
        return new HubRoutePathResponse(
                sourceHubId,
                destinationHubId,
                path.totalDistanceMeters(),
                path.totalDurationSeconds(),
                segments);
    }

    public record Segment(
            int sequence,
            UUID hubRouteId,
            UUID sourceHubId,
            UUID destinationHubId,
            long distanceMeters,
            long durationSeconds
    ) {

        private static Segment from(int sequence, HubRoute route) {
            return new Segment(
                    sequence,
                    route.getId(),
                    route.getSourceHubId(),
                    route.getDestinationHubId(),
                    route.getDistanceMeters(),
                    route.getDurationSeconds());
        }
    }
}
