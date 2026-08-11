package com.logistics.hubservice.presentation.hubroute.dto;

import com.logistics.hubservice.application.hubroute.command.CreateHubRouteCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record CreateHubRouteRequest(
        @NotNull(message = "출발 허브 ID는 필수입니다.")
        UUID sourceHubId,
        @NotNull(message = "도착 허브 ID는 필수입니다.")
        UUID destinationHubId,
        @NotNull(message = "이동 거리는 필수입니다.")
        @Positive(message = "이동 거리는 0보다 커야 합니다.")
        Long distanceMeters,
        @NotNull(message = "소요 시간은 필수입니다.")
        @Positive(message = "소요 시간은 0보다 커야 합니다.")
        Long durationSeconds
) {

    public CreateHubRouteCommand toCommand() {
        return new CreateHubRouteCommand(
                sourceHubId, destinationHubId, distanceMeters, durationSeconds);
    }
}
