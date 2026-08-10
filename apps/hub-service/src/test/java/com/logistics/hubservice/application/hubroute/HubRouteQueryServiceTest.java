package com.logistics.hubservice.application.hubroute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.logistics.common.exception.BusinessException;
import com.logistics.hubservice.application.hub.HubErrorCode;
import com.logistics.hubservice.application.hubroute.dto.HubRouteResponse;
import com.logistics.hubservice.application.hubroute.query.HubRouteQueryService;
import com.logistics.hubservice.domain.hubroute.HubRoute;
import com.logistics.hubservice.domain.hubroute.HubRouteRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

class HubRouteQueryServiceTest {

    private static final UUID HUB_ROUTE_ID =
            UUID.fromString("821e9d59-ea65-475f-abf9-86d7c79f0286");
    private static final UUID SOURCE_HUB_ID =
            UUID.fromString("01b6e9a4-5d93-4c22-b7ce-cb2f60c403d6");
    private static final UUID DESTINATION_HUB_ID =
            UUID.fromString("b44a6de8-51ae-4f34-b3ad-a484ae85583c");

    private InMemoryHubRouteRepository repository;
    private HubRouteQueryService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryHubRouteRepository();
        service = new HubRouteQueryService(repository);
    }

    @Test
    @DisplayName("활성 허브 경로를 ID로 조회한다")
    void getOneReturnsAnActiveRoute() {
        repository.save(activeRoute());

        HubRouteResponse response = service.getOne(HUB_ROUTE_ID);

        assertThat(response.hubRouteId()).isEqualTo(HUB_ROUTE_ID);
        assertThat(response.sourceHubId()).isEqualTo(SOURCE_HUB_ID);
        assertThat(response.destinationHubId()).isEqualTo(DESTINATION_HUB_ID);
        assertThat(response.distanceMeters()).isEqualTo(123_400L);
        assertThat(response.durationSeconds()).isEqualTo(7_200L);
    }

    @Test
    @DisplayName("존재하지 않거나 삭제된 경로를 조회하면 경로를 찾을 수 없다")
    void getOneRejectsMissingOrDeletedRouteWithHub002() {
        HubRoute deletedRoute = activeRoute();
        ReflectionTestUtils.setField(deletedRoute, "deletedAt", LocalDateTime.now());
        repository.save(deletedRoute);

        assertHubRouteNotFound(() -> service.getOne(HUB_ROUTE_ID));
        assertHubRouteNotFound(() -> service.getOne(UUID.randomUUID()));
    }

    @Test
    @DisplayName("출발 허브와 도착 허브 조건을 조합하면 활성 경로만 검색한다")
    void searchCombinesSourceAndDestinationConditionsAndExcludesDeletedRoutes() {
        HubRoute matchingRoute = activeRoute();
        HubRoute sourceOnlyRoute = route(
                UUID.randomUUID(), SOURCE_HUB_ID, UUID.randomUUID(), LocalDateTime.of(2026, 8, 9, 9, 30));
        HubRoute destinationOnlyRoute = route(
                UUID.randomUUID(), UUID.randomUUID(), DESTINATION_HUB_ID, LocalDateTime.of(2026, 8, 9, 10, 0));
        HubRoute deletedRoute = route(
                UUID.randomUUID(), SOURCE_HUB_ID, DESTINATION_HUB_ID, LocalDateTime.of(2026, 8, 9, 10, 30));
        ReflectionTestUtils.setField(deletedRoute, "deletedAt", LocalDateTime.now());
        repository.save(matchingRoute);
        repository.save(sourceOnlyRoute);
        repository.save(destinationOnlyRoute);
        repository.save(deletedRoute);

        Page<HubRouteResponse> responsePage = service.search(
                SOURCE_HUB_ID,
                DESTINATION_HUB_ID,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        assertThat(responsePage.getContent())
                .extracting(HubRouteResponse::hubRouteId)
                .containsExactly(HUB_ROUTE_ID);
    }

    @Test
    @DisplayName("지원하지 않는 페이지 크기는 10으로 보정한다")
    void searchNormalizesUnsupportedPageSizeToTen() {
        Page<HubRouteResponse> responsePage = service.search(
                null,
                null,
                PageRequest.of(0, 25, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        assertThat(responsePage.getSize()).isEqualTo(10);
    }

    @Test
    @DisplayName("허용하지 않는 정렬 필드로 허브 경로를 검색할 수 없다")
    void searchRejectsUnsupportedSortProperty() {
        assertThatThrownBy(() -> service.search(
                null,
                null,
                PageRequest.of(0, 10, Sort.by("sourceHubId"))
        ))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(com.logistics.common.error.CommonErrorCode.INVALID_INPUT));
    }

    private static void assertHubRouteNotFound(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(HubErrorCode.HUB_ROUTE_NOT_FOUND));
    }

    private static HubRoute activeRoute() {
        return route(HUB_ROUTE_ID, SOURCE_HUB_ID, DESTINATION_HUB_ID, LocalDateTime.of(2026, 8, 9, 9, 0));
    }

    private static HubRoute route(
            UUID routeId, UUID sourceHubId, UUID destinationHubId, LocalDateTime createdAt) {
        HubRoute route = HubRoute.create(
                sourceHubId,
                destinationHubId,
                123_400L,
                7_200L
        );
        ReflectionTestUtils.setField(route, "id", routeId);
        ReflectionTestUtils.setField(route, "createdAt", createdAt);
        ReflectionTestUtils.setField(route, "updatedAt", createdAt.plusHours(1));
        return route;
    }

    private static final class InMemoryHubRouteRepository implements HubRouteRepository {

        private final Map<UUID, HubRoute> routes = new LinkedHashMap<>();

        @Override
        public HubRoute save(HubRoute hubRoute) {
            routes.put(hubRoute.getId(), hubRoute);
            return hubRoute;
        }

        @Override
        public Optional<HubRoute> findByIdAndDeletedAtIsNull(UUID id) {
            return Optional.ofNullable(routes.get(id))
                    .filter(route -> route.getDeletedAt() == null);
        }

        @Override
        public Page<HubRoute> search(UUID sourceHubId, UUID destinationHubId, Pageable pageable) {
            List<HubRoute> matchingRoutes = routes.values().stream()
                    .filter(route -> route.getDeletedAt() == null)
                    .filter(route -> sourceHubId == null || route.getSourceHubId().equals(sourceHubId))
                    .filter(route -> destinationHubId == null || route.getDestinationHubId().equals(destinationHubId))
                    .toList();
            int startIndex = (int) pageable.getOffset();
            if (startIndex >= matchingRoutes.size()) {
                return new PageImpl<>(List.of(), pageable, matchingRoutes.size());
            }
            int endIndex = Math.min(startIndex + pageable.getPageSize(), matchingRoutes.size());
            return new PageImpl<>(
                    new ArrayList<>(matchingRoutes.subList(startIndex, endIndex)),
                    pageable,
                    matchingRoutes.size()
            );
        }

        @Override
        public boolean existsBySourceHubIdAndDestinationHubIdAndDeletedAtIsNull(
                UUID sourceHubId, UUID destinationHubId) {
            return routes.values().stream()
                    .anyMatch(route -> route.getDeletedAt() == null
                            && route.getSourceHubId().equals(sourceHubId)
                            && route.getDestinationHubId().equals(destinationHubId));
        }
    }
}
