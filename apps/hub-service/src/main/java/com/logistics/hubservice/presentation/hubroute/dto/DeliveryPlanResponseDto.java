package com.logistics.hubservice.presentation.hubroute.dto;

import com.logistics.hubservice.application.hubroute.dto.HubRoutePathResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

public record DeliveryPlanResponseDto(
        UUID companyDeliveryManagerId,
        List<RouteResponse> routes
) {

    private static final BigDecimal METERS_PER_KM = BigDecimal.valueOf(1000);

    public DeliveryPlanResponseDto {
        routes = List.copyOf(routes);
    }

    public static DeliveryPlanResponseDto from(HubRoutePathResponse path) {
        return new DeliveryPlanResponseDto(
                null,
                path.segments().stream()
                        .map(RouteResponse::from)
                        .toList());
    }

    public record RouteResponse(
            Integer sequence,
            UUID departureHubId,
            UUID arrivalHubId,
            BigDecimal estimatedDistanceKm,
            Integer estimatedDurationMinutes,
            UUID hubDeliveryManagerId
    ) {

        private static RouteResponse from(HubRoutePathResponse.Segment segment) {
            return new RouteResponse(
                    segment.sequence(),
                    segment.sourceHubId(),
                    segment.destinationHubId(),
                    toKilometers(segment.distanceMeters()),
                    toMinutes(segment.durationSeconds()),
                    null);
        }

        private static BigDecimal toKilometers(long distanceMeters) {
            return BigDecimal.valueOf(distanceMeters)
                    .divide(METERS_PER_KM, 3, RoundingMode.HALF_UP);
        }

        private static int toMinutes(long durationSeconds) {
            if (durationSeconds <= 0) {
                return 0;
            }
            return (int) Math.ceil(durationSeconds / 60.0);
        }
    }
}
