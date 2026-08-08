package com.logistics.hubservice.domain.hubroute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HubRouteTest {

    private static final UUID SOURCE_HUB_ID =
            UUID.fromString("01b6e9a4-5d93-4c22-b7ce-cb2f60c403d6");
    private static final UUID DESTINATION_HUB_ID =
            UUID.fromString("b44a6de8-51ae-4f34-b3ad-a484ae85583c");

    @Test
    @DisplayName("허브 경로를 생성하면 방향성과 이동 정보를 보존한다")
    void createStoresDirectionalRouteValues() {
        HubRoute hubRoute = HubRoute.create(
                SOURCE_HUB_ID,
                DESTINATION_HUB_ID,
                123_400L,
                7_200L
        );

        assertThat(hubRoute.getSourceHubId()).isEqualTo(SOURCE_HUB_ID);
        assertThat(hubRoute.getDestinationHubId()).isEqualTo(DESTINATION_HUB_ID);
        assertThat(hubRoute.getDistanceMeters()).isEqualTo(123_400L);
        assertThat(hubRoute.getDurationSeconds()).isEqualTo(7_200L);
        assertThat(hubRoute.getDeletedAt()).isNull();
        assertThat(hubRoute.getDeletedBy()).isNull();
    }

    @Test
    @DisplayName("출발 허브와 도착 허브가 동일하면 경로를 생성할 수 없다")
    void createRejectsTheSameSourceAndDestinationHub() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> HubRoute.create(SOURCE_HUB_ID, SOURCE_HUB_ID, 1L, 1L))
                .withMessage("출발 허브와 도착 허브는 달라야 합니다.");
    }

    @Test
    @DisplayName("이동 거리가 0 이하이면 경로를 생성할 수 없다")
    void createRejectsNonPositiveDistance() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> HubRoute.create(SOURCE_HUB_ID, DESTINATION_HUB_ID, 0L, 1L))
                .withMessage("이동 거리는 0보다 커야 합니다.");
    }

    @Test
    @DisplayName("소요 시간이 0 이하이면 경로를 생성할 수 없다")
    void createRejectsNonPositiveDuration() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> HubRoute.create(SOURCE_HUB_ID, DESTINATION_HUB_ID, 1L, 0L))
                .withMessage("소요 시간은 0보다 커야 합니다.");
    }
}
