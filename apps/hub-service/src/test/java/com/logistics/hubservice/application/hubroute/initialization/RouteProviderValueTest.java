package com.logistics.hubservice.application.hubroute.initialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RouteProviderValueTest {

    @Test
    @DisplayName("Hub 좌표는 DB 정밀도에 맞춰 소수점 일곱 자리로 정규화한다")
    void normalizesHubCoordinatesToSevenDecimalPlaces() {
        HubCoordinates coordinates = new HubCoordinates(
                new BigDecimal("37.12345678"),
                new BigDecimal("127.98765432"));

        assertThat(coordinates.latitude()).isEqualByComparingTo("37.1234568");
        assertThat(coordinates.longitude()).isEqualByComparingTo("127.9876543");
    }

    @Test
    @DisplayName("위도와 경도의 허용 범위를 벗어난 좌표는 거부한다")
    void rejectsCoordinatesOutsideTheEarthRange() {
        assertThatThrownBy(() -> new HubCoordinates(new BigDecimal("90.1"), BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new HubCoordinates(BigDecimal.ZERO, new BigDecimal("-180.1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("경로 거리와 소요 시간은 모두 양수여야 한다")
    void rejectsNonPositiveRouteMetrics() {
        assertThatThrownBy(() -> new RouteMetric(0L, 1L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RouteMetric(1L, 0L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
