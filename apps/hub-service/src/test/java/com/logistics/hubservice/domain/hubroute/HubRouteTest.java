package com.logistics.hubservice.domain.hubroute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.time.LocalDateTime;
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

    @Test
    @DisplayName("이동 거리만 수정하면 소요 시간과 출발 및 도착 허브는 유지한다")
    void updateDistancePreservesDurationAndDirectionalHubs() {
        HubRoute hubRoute = HubRoute.create(
                SOURCE_HUB_ID,
                DESTINATION_HUB_ID,
                123_400L,
                7_200L
        );

        hubRoute.update(130_000L, null);

        assertThat(hubRoute.getSourceHubId()).isEqualTo(SOURCE_HUB_ID);
        assertThat(hubRoute.getDestinationHubId()).isEqualTo(DESTINATION_HUB_ID);
        assertThat(hubRoute.getDistanceMeters()).isEqualTo(130_000L);
        assertThat(hubRoute.getDurationSeconds()).isEqualTo(7_200L);
    }

    @Test
    @DisplayName("소요 시간만 수정하면 이동 거리와 출발 및 도착 허브는 유지한다")
    void updateDurationPreservesDistanceAndDirectionalHubs() {
        HubRoute hubRoute = HubRoute.create(
                SOURCE_HUB_ID,
                DESTINATION_HUB_ID,
                123_400L,
                7_200L
        );

        hubRoute.update(null, 7_500L);

        assertThat(hubRoute.getSourceHubId()).isEqualTo(SOURCE_HUB_ID);
        assertThat(hubRoute.getDestinationHubId()).isEqualTo(DESTINATION_HUB_ID);
        assertThat(hubRoute.getDistanceMeters()).isEqualTo(123_400L);
        assertThat(hubRoute.getDurationSeconds()).isEqualTo(7_500L);
    }

    @Test
    @DisplayName("이동 거리를 0 이하로 수정할 수 없다")
    void updateRejectsNonPositiveDistance() {
        HubRoute hubRoute = HubRoute.create(
                SOURCE_HUB_ID,
                DESTINATION_HUB_ID,
                123_400L,
                7_200L
        );

        assertThatIllegalArgumentException()
                .isThrownBy(() -> hubRoute.update(0L, null))
                .withMessage("이동 거리는 0보다 커야 합니다.");
    }

    @Test
    @DisplayName("소요 시간을 0 이하로 수정할 수 없다")
    void updateRejectsNonPositiveDuration() {
        HubRoute hubRoute = HubRoute.create(
                SOURCE_HUB_ID,
                DESTINATION_HUB_ID,
                123_400L,
                7_200L
        );

        assertThatIllegalArgumentException()
                .isThrownBy(() -> hubRoute.update(null, 0L))
                .withMessage("소요 시간은 0보다 커야 합니다.");
    }

    @Test
    @DisplayName("활성 허브 경로를 삭제하면 삭제 시각과 요청자를 기록한다")
    void deleteRecordsDeletionTimeAndActor() {
        HubRoute hubRoute = HubRoute.create(
                SOURCE_HUB_ID,
                DESTINATION_HUB_ID,
                123_400L,
                7_200L
        );
        UUID deletedBy = UUID.fromString("e81cce60-2e94-41cd-9b89-dbf7dfc5f9b5");

        hubRoute.delete(deletedBy);

        assertThat(hubRoute.getDeletedAt()).isNotNull();
        assertThat(hubRoute.getDeletedBy()).isEqualTo(deletedBy);
    }

    @Test
    @DisplayName("논리 삭제된 허브 경로는 다시 삭제하거나 수정할 수 없다")
    void deletedRouteRejectsFurtherDeletionAndUpdate() {
        HubRoute hubRoute = HubRoute.create(
                SOURCE_HUB_ID,
                DESTINATION_HUB_ID,
                123_400L,
                7_200L
        );
        UUID originalDeletedBy = UUID.fromString("e81cce60-2e94-41cd-9b89-dbf7dfc5f9b5");
        hubRoute.delete(originalDeletedBy);
        LocalDateTime originalDeletedAt = hubRoute.getDeletedAt();

        assertThatIllegalStateException()
                .isThrownBy(() -> hubRoute.delete(UUID.randomUUID()))
                .withMessage("삭제된 허브 경로는 변경할 수 없습니다.");
        assertThatIllegalStateException()
                .isThrownBy(() -> hubRoute.update(130_000L, null))
                .withMessage("삭제된 허브 경로는 변경할 수 없습니다.");
        assertThat(hubRoute.getDeletedAt()).isEqualTo(originalDeletedAt);
        assertThat(hubRoute.getDeletedBy()).isEqualTo(originalDeletedBy);
        assertThat(hubRoute.getDistanceMeters()).isEqualTo(123_400L);
    }
}
