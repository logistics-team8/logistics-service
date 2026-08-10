package com.logistics.hubservice.domain.hubroute;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class HubRoutePathFinderTest {

    private static final UUID HUB_A = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID HUB_B = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID HUB_C = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID HUB_D = UUID.fromString("00000000-0000-0000-0000-000000000004");

    private final HubRoutePathFinder pathFinder = new HubRoutePathFinder();

    @Test
    @DisplayName("직접 연결된 허브 사이의 단일 구간을 찾는다")
    void findsDirectRoute() {
        HubRoute directRoute = route(101, HUB_A, HUB_B, 10L, 20L);

        HubRoutePath path = pathFinder.findShortestPath(List.of(directRoute), HUB_A, HUB_B)
                .orElseThrow();

        assertThat(path.segments()).containsExactly(directRoute);
        assertThat(path.totalDistanceMeters()).isEqualTo(10L);
        assertThat(path.totalDurationSeconds()).isEqualTo(20L);
    }

    @Test
    @DisplayName("여러 구간을 연결해 도착 허브까지의 경로를 찾는다")
    void findsMultiSegmentRoute() {
        HubRoute first = route(101, HUB_A, HUB_B, 10L, 20L);
        HubRoute second = route(102, HUB_B, HUB_C, 20L, 30L);

        HubRoutePath path = pathFinder.findShortestPath(List.of(second, first), HUB_A, HUB_C)
                .orElseThrow();

        assertThat(path.segments()).containsExactly(first, second);
        assertThat(path.totalDistanceMeters()).isEqualTo(30L);
        assertThat(path.totalDurationSeconds()).isEqualTo(50L);
    }

    @Test
    @DisplayName("소요 시간이 길어도 이동 거리 합계가 짧은 경로를 선택한다")
    void prioritizesTotalDistanceOverDuration() {
        HubRoute shortFirst = route(101, HUB_A, HUB_B, 5L, 100L);
        HubRoute shortSecond = route(102, HUB_B, HUB_D, 5L, 100L);
        HubRoute fastButLong = route(103, HUB_A, HUB_D, 11L, 1L);

        HubRoutePath path = pathFinder.findShortestPath(
                        List.of(fastButLong, shortSecond, shortFirst),
                        HUB_A,
                        HUB_D)
                .orElseThrow();

        assertThat(path.segments()).containsExactly(shortFirst, shortSecond);
        assertThat(path.totalDistanceMeters()).isEqualTo(10L);
        assertThat(path.totalDurationSeconds()).isEqualTo(200L);
    }

    @Test
    @DisplayName("이동 거리 합계가 같으면 소요 시간 합계가 짧은 경로를 선택한다")
    void usesTotalDurationAsDistanceTieBreaker() {
        HubRoute slowFirst = route(101, HUB_A, HUB_B, 5L, 100L);
        HubRoute slowSecond = route(102, HUB_B, HUB_D, 5L, 100L);
        HubRoute fastFirst = route(103, HUB_A, HUB_C, 4L, 10L);
        HubRoute fastSecond = route(104, HUB_C, HUB_D, 6L, 10L);

        HubRoutePath path = pathFinder.findShortestPath(
                        List.of(slowFirst, slowSecond, fastFirst, fastSecond),
                        HUB_A,
                        HUB_D)
                .orElseThrow();

        assertThat(path.segments()).containsExactly(fastFirst, fastSecond);
        assertThat(path.totalDistanceMeters()).isEqualTo(10L);
        assertThat(path.totalDurationSeconds()).isEqualTo(20L);
    }

    @Test
    @DisplayName("방향이 반대인 경로는 역방향 이동에 사용할 수 없다")
    void preservesRouteDirection() {
        HubRoute forwardRoute = route(101, HUB_A, HUB_B, 10L, 20L);

        assertThat(pathFinder.findShortestPath(List.of(forwardRoute), HUB_B, HUB_A)).isEmpty();
    }

    @Test
    @DisplayName("순환 경로가 있어도 방문 가능한 도착 허브의 최단 경로를 찾는다")
    void findsPathInCyclicGraph() {
        HubRoute first = route(101, HUB_A, HUB_B, 10L, 20L);
        HubRoute cycle = route(102, HUB_B, HUB_A, 10L, 20L);
        HubRoute second = route(103, HUB_B, HUB_C, 10L, 20L);

        HubRoutePath path = pathFinder.findShortestPath(
                        List.of(first, cycle, second),
                        HUB_A,
                        HUB_C)
                .orElseThrow();

        assertThat(path.segments()).containsExactly(first, second);
    }

    @Test
    @DisplayName("연결되지 않은 허브 사이에는 경로를 반환하지 않는다")
    void returnsEmptyWhenDestinationIsUnreachable() {
        HubRoute unrelatedRoute = route(101, HUB_A, HUB_B, 10L, 20L);

        assertThat(pathFinder.findShortestPath(List.of(unrelatedRoute), HUB_A, HUB_D)).isEmpty();
    }

    @Test
    @DisplayName("출발 허브와 도착 허브가 같으면 이동 구간과 합계가 0이다")
    void returnsEmptyPathForSameHub() {
        HubRoutePath path = pathFinder.findShortestPath(List.of(), HUB_A, HUB_A)
                .orElseThrow();

        assertThat(path.segments()).isEmpty();
        assertThat(path.totalDistanceMeters()).isZero();
        assertThat(path.totalDurationSeconds()).isZero();
    }

    private HubRoute route(
            int routeNumber,
            UUID sourceHubId,
            UUID destinationHubId,
            long distanceMeters,
            long durationSeconds) {
        HubRoute route = HubRoute.create(
                sourceHubId,
                destinationHubId,
                distanceMeters,
                durationSeconds);
        ReflectionTestUtils.setField(
                route,
                "id",
                UUID.fromString("00000000-0000-0000-0000-%012d".formatted(routeNumber)));
        return route;
    }
}
