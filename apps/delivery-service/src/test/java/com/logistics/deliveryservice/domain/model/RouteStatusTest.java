package com.logistics.deliveryservice.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RouteStatusTest {

    @Test
    void definesRouteStatusesInProgressOrder() {
        assertThat(RouteStatus.values()).containsExactly(
                RouteStatus.WAITING,
                RouteStatus.MOVING,
                RouteStatus.ARRIVED,
                RouteStatus.COMPLETED,
                RouteStatus.FAILED
        );
    }
}
