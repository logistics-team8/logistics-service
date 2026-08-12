package com.logistics.hubservice.presentation.hubroute.dto;

import com.logistics.hubservice.application.hubroute.command.UpdateHubRouteCommand;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;

public record UpdateHubRouteRequest(
        @Positive(message = "이동 거리는 0보다 커야 합니다.")
        Long distanceMeters,
        @Positive(message = "소요 시간은 0보다 커야 합니다.")
        Long durationSeconds
) {

    @AssertTrue(message = "수정할 이동 거리나 소요 시간을 하나 이상 입력해야 합니다.")
    public boolean isUpdateRequested() {
        return distanceMeters != null || durationSeconds != null;
    }

    public UpdateHubRouteCommand toCommand() {
        return new UpdateHubRouteCommand(distanceMeters, durationSeconds);
    }
}
