package com.logistics.hubservice.domain.hubroute;

import com.logistics.hubservice.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;

@Getter
@Entity
@Table(name = "p_hub_routes")
public class HubRoute extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "source_hub_id", nullable = false)
    private UUID sourceHubId;

    @Column(name = "destination_hub_id", nullable = false)
    private UUID destinationHubId;

    @Column(name = "distance_meters", nullable = false)
    private long distanceMeters;

    @Column(name = "duration_seconds", nullable = false)
    private long durationSeconds;

    private LocalDateTime deletedAt;
    private UUID deletedBy;

    protected HubRoute() {
    }

    private HubRoute(
            UUID sourceHubId,
            UUID destinationHubId,
            long distanceMeters,
            long durationSeconds) {
        validate(sourceHubId, destinationHubId, distanceMeters, durationSeconds);
        this.sourceHubId = sourceHubId;
        this.destinationHubId = destinationHubId;
        this.distanceMeters = distanceMeters;
        this.durationSeconds = durationSeconds;
    }

    public static HubRoute create(
            UUID sourceHubId,
            UUID destinationHubId,
            long distanceMeters,
            long durationSeconds) {
        return new HubRoute(sourceHubId, destinationHubId, distanceMeters, durationSeconds);
    }

    public void update(Long distanceMeters, Long durationSeconds) {
        if (distanceMeters != null) {
            validateDistance(distanceMeters);
            this.distanceMeters = distanceMeters;
        }
        if (durationSeconds != null) {
            validateDuration(durationSeconds);
            this.durationSeconds = durationSeconds;
        }
    }

    private static void validate(
            UUID sourceHubId,
            UUID destinationHubId,
            long distanceMeters,
            long durationSeconds) {
        if (sourceHubId == null || destinationHubId == null) {
            throw new IllegalArgumentException("출발 허브와 도착 허브는 필수입니다.");
        }
        if (sourceHubId.equals(destinationHubId)) {
            throw new IllegalArgumentException("출발 허브와 도착 허브는 달라야 합니다.");
        }
        validateDistance(distanceMeters);
        validateDuration(durationSeconds);
    }

    private static void validateDistance(long distanceMeters) {
        if (distanceMeters <= 0) {
            throw new IllegalArgumentException("이동 거리는 0보다 커야 합니다.");
        }
    }

    private static void validateDuration(long durationSeconds) {
        if (durationSeconds <= 0) {
            throw new IllegalArgumentException("소요 시간은 0보다 커야 합니다.");
        }
    }
}
