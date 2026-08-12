package com.logistics.hubservice.presentation.hubroute.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.hubservice.application.hubroute.dto.HubRoutePathResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DeliveryPlanResponseDtoTest {

    private static final UUID SOURCE_HUB_ID =
            UUID.fromString("0804066f-aadd-4b05-b7dc-7b0132040c4c");
    private static final UUID MIDDLE_HUB_ID =
            UUID.fromString("6b9cbad9-a9a6-453f-a124-d6de035fd214");
    private static final UUID DESTINATION_HUB_ID =
            UUID.fromString("5aee128f-3f59-43f5-95b3-bd3638c4d968");
    private static final UUID FIRST_ROUTE_ID =
            UUID.fromString("1194b39d-572d-4883-8577-859ddf06a5a1");
    private static final UUID SECOND_ROUTE_ID =
            UUID.fromString("8ec02ebd-7d84-4822-9172-b356947ee1e6");

    @Test
    @DisplayName("최단 경로 구간을 Delivery 계약의 km/분 단위로 변환한다")
    void mapsSegmentsToDeliveryPlanRoutes() {
        HubRoutePathResponse path = new HubRoutePathResponse(
                SOURCE_HUB_ID,
                DESTINATION_HUB_ID,
                100L,
                120L,
                List.of(
                        new HubRoutePathResponse.Segment(
                                1, FIRST_ROUTE_ID, SOURCE_HUB_ID, MIDDLE_HUB_ID, 40L, 50L),
                        new HubRoutePathResponse.Segment(
                                2, SECOND_ROUTE_ID, MIDDLE_HUB_ID, DESTINATION_HUB_ID, 60L, 70L)));

        DeliveryPlanResponseDto response = DeliveryPlanResponseDto.from(path);

        assertThat(response.routes()).hasSize(2);
        assertThat(response.routes().get(0)).isEqualTo(new DeliveryPlanResponseDto.RouteResponse(
                1,
                SOURCE_HUB_ID,
                MIDDLE_HUB_ID,
                new BigDecimal("0.040"),
                1));
        assertThat(response.routes().get(1)).isEqualTo(new DeliveryPlanResponseDto.RouteResponse(
                2,
                MIDDLE_HUB_ID,
                DESTINATION_HUB_ID,
                new BigDecimal("0.060"),
                2));
    }

    @Test
    @DisplayName("같은 허브 경로는 빈 구간 목록을 반환한다")
    void mapsEmptyPathToEmptyRoutes() {
        HubRoutePathResponse path = new HubRoutePathResponse(
                SOURCE_HUB_ID,
                SOURCE_HUB_ID,
                0L,
                0L,
                List.of());

        DeliveryPlanResponseDto response = DeliveryPlanResponseDto.from(path);

        assertThat(response.routes()).isEmpty();
    }
}
