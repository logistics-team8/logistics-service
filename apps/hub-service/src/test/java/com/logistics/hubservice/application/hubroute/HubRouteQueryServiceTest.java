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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

    private static void assertHubRouteNotFound(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(HubErrorCode.HUB_ROUTE_NOT_FOUND));
    }

    private static HubRoute activeRoute() {
        HubRoute route = HubRoute.create(
                SOURCE_HUB_ID,
                DESTINATION_HUB_ID,
                123_400L,
                7_200L
        );
        ReflectionTestUtils.setField(route, "id", HUB_ROUTE_ID);
        ReflectionTestUtils.setField(route, "createdAt", LocalDateTime.of(2026, 8, 9, 9, 0));
        ReflectionTestUtils.setField(route, "updatedAt", LocalDateTime.of(2026, 8, 9, 10, 0));
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
        public boolean existsBySourceHubIdAndDestinationHubIdAndDeletedAtIsNull(
                UUID sourceHubId, UUID destinationHubId) {
            return routes.values().stream()
                    .anyMatch(route -> route.getDeletedAt() == null
                            && route.getSourceHubId().equals(sourceHubId)
                            && route.getDestinationHubId().equals(destinationHubId));
        }
    }
}
