package com.logistics.hubservice.application.hubroute.initialization;

public record RouteMetric(long distanceMeters, long durationSeconds) {

    public RouteMetric {
        if (distanceMeters <= 0) {
            throw new IllegalArgumentException("이동 거리는 0보다 커야 합니다.");
        }
        if (durationSeconds <= 0) {
            throw new IllegalArgumentException("소요 시간은 0보다 커야 합니다.");
        }
    }
}
