package com.logistics.hubservice.presentation.hubroute.dto;

import com.logistics.hubservice.application.hubroute.dto.HubRoutePathResponse;
import java.util.List;
import java.util.UUID;

public record HubRoutePathResponseDto(
        UUID sourceHubId,
        UUID destinationHubId,
        long totalDistanceMeters,
        long totalDurationSeconds,
        List<SegmentDto> segments
) {

    public HubRoutePathResponseDto {
        segments = List.copyOf(segments);
    }

    public static HubRoutePathResponseDto from(HubRoutePathResponse response) {
        return new HubRoutePathResponseDto(
                response.sourceHubId(),
                response.destinationHubId(),
                response.totalDistanceMeters(),
                response.totalDurationSeconds(),
                response.segments().stream()
                        .map(SegmentDto::from)
                        .toList());
    }

    public record SegmentDto(
            int sequence,
            UUID hubRouteId,
            UUID sourceHubId,
            UUID destinationHubId,
            long distanceMeters,
            long durationSeconds
    ) {

        private static SegmentDto from(HubRoutePathResponse.Segment segment) {
            return new SegmentDto(
                    segment.sequence(),
                    segment.hubRouteId(),
                    segment.sourceHubId(),
                    segment.destinationHubId(),
                    segment.distanceMeters(),
                    segment.durationSeconds());
        }
    }
}
