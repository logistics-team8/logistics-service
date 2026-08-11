package com.logistics.hubservice.application.hubroute.initialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.logistics.hubservice.domain.hub.Hub;
import com.logistics.hubservice.domain.hub.HubRepository;
import com.logistics.hubservice.domain.hubroute.HubRoute;
import com.logistics.hubservice.domain.hubroute.HubRouteRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

class HubRouteDefaultDataServiceTest {

    private InMemoryHubRepository hubRepository;
    private InMemoryHubRouteRepository hubRouteRepository;
    private FakeHubLocationProvider locationProvider;
    private FakeRouteMetricProvider metricProvider;
    private HubRouteDefaultDataService service;

    @BeforeEach
    void setUp() {
        hubRepository = new InMemoryHubRepository();
        hubRouteRepository = new InMemoryHubRouteRepository();
        locationProvider = new FakeHubLocationProvider();
        metricProvider = new FakeRouteMetricProvider();
        service = new HubRouteDefaultDataService(
                hubRepository,
                hubRouteRepository,
                locationProvider,
                metricProvider);
    }

    @Test
    @DisplayName("첫 실행에서 17개 고정 Hub와 36개 방향 경로를 모두 생성한다")
    void createsAllDefaultHubsAndDirectionalRoutes() {
        HubRouteDefaultDataResult result = service.initialize();

        assertThat(result.createdHubCount()).isEqualTo(17);
        assertThat(result.createdHubRouteCount()).isEqualTo(36);
        assertThat(hubRepository.hubs).hasSize(17);
        assertThat(hubRepository.hubs.keySet())
                .containsExactlyInAnyOrder(Arrays.stream(DefaultHub.values())
                        .map(DefaultHub::hubId)
                        .toArray(UUID[]::new));
        assertThat(hubRepository.hubs.values())
                .allSatisfy(hub -> {
                    assertThat(hub.getName()).isNotBlank();
                    assertThat(hub.getAddress()).isNotBlank();
                    assertThat(hub.getLatitude()).isNotNull();
                    assertThat(hub.getLongitude()).isNotNull();
                });
        assertThat(locationProvider.requestedAddresses)
                .containsExactly(Arrays.stream(DefaultHub.values())
                        .map(DefaultHub::address)
                        .toArray(String[]::new));
        assertThat(metricProvider.requests).hasSize(36);
        assertThat(hubRouteRepository.routes.keySet())
                .containsExactlyInAnyOrderElementsOf(DefaultHubRouteTopology.directedConnections());
    }

    @Test
    @DisplayName("이미 생성된 Hub와 방향 경로가 있으면 외부 API를 다시 호출하거나 중복 저장하지 않는다")
    void isIdempotentWhenAllDefaultDataAlreadyExists() {
        service.initialize();
        locationProvider.requestedAddresses.clear();
        metricProvider.requests.clear();

        HubRouteDefaultDataResult result = service.initialize();

        assertThat(result.createdHubCount()).isZero();
        assertThat(result.createdHubRouteCount()).isZero();
        assertThat(hubRepository.hubs).hasSize(17);
        assertThat(hubRouteRepository.routes).hasSize(36);
        assertThat(locationProvider.requestedAddresses).isEmpty();
        assertThat(metricProvider.requests).isEmpty();
    }

    @Test
    @DisplayName("Directions 호출이 중간에 실패하면 Hub와 경로를 하나도 저장하지 않는다")
    void doesNotPersistPartialDataWhenMetricLookupFails() {
        metricProvider.failureRequestNumber = 10;

        assertThatThrownBy(service::initialize)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Directions 실패");
        assertThat(hubRepository.hubs).isEmpty();
        assertThat(hubRouteRepository.routes).isEmpty();
    }

    private static final class FakeHubLocationProvider implements HubLocationProvider {

        private final List<String> requestedAddresses = new ArrayList<>();

        @Override
        public HubCoordinates geocode(String address) {
            requestedAddresses.add(address);
            int index = requestedAddresses.size();
            return new HubCoordinates(
                    new BigDecimal("35").add(new BigDecimal("0.1").multiply(BigDecimal.valueOf(index))),
                    new BigDecimal("126").add(new BigDecimal("0.1").multiply(BigDecimal.valueOf(index))));
        }
    }

    private static final class FakeRouteMetricProvider implements RouteMetricProvider {

        private final List<MetricRequest> requests = new ArrayList<>();
        private int failureRequestNumber = -1;

        @Override
        public RouteMetric getMetric(HubCoordinates source, HubCoordinates destination) {
            requests.add(new MetricRequest(source, destination));
            if (requests.size() == failureRequestNumber) {
                throw new IllegalStateException("Directions 실패");
            }
            return new RouteMetric(1_000L + requests.size(), 100L + requests.size());
        }
    }

    private record MetricRequest(HubCoordinates source, HubCoordinates destination) {
    }

    private static final class InMemoryHubRepository implements HubRepository {

        private final Map<UUID, Hub> hubs = new LinkedHashMap<>();

        @Override
        public Hub save(Hub hub) {
            hubs.put(hub.getId(), hub);
            return hub;
        }

        @Override
        public Optional<Hub> findByIdAndDeletedAtIsNull(UUID id) {
            return Optional.ofNullable(hubs.get(id))
                    .filter(hub -> hub.getDeletedAt() == null);
        }

        @Override
        public Page<Hub> findAllByDeletedAtIsNull(Pageable pageable) {
            return Page.empty(pageable);
        }

        @Override
        public Page<Hub> search(String keyword, Pageable pageable) {
            return Page.empty(pageable);
        }
    }

    private static final class InMemoryHubRouteRepository implements HubRouteRepository {

        private final Map<DefaultHubConnection, HubRoute> routes = new LinkedHashMap<>();
        private final Map<UUID, DefaultHub> defaultHubById = Arrays.stream(DefaultHub.values())
                .collect(java.util.stream.Collectors.toMap(DefaultHub::hubId, hub -> hub));

        @Override
        public HubRoute save(HubRoute hubRoute) {
            DefaultHub source = defaultHubById.get(hubRoute.getSourceHubId());
            DefaultHub destination = defaultHubById.get(hubRoute.getDestinationHubId());
            routes.put(new DefaultHubConnection(source, destination), hubRoute);
            return hubRoute;
        }

        @Override
        public Optional<HubRoute> findByIdAndDeletedAtIsNull(UUID id) {
            return Optional.empty();
        }

        @Override
        public List<HubRoute> findAllByDeletedAtIsNull() {
            return List.copyOf(routes.values());
        }

        @Override
        public Page<HubRoute> search(UUID sourceHubId, UUID destinationHubId, Pageable pageable) {
            return Page.empty(pageable);
        }

        @Override
        public boolean existsBySourceHubIdAndDestinationHubIdAndDeletedAtIsNull(
                UUID sourceHubId, UUID destinationHubId) {
            return routes.containsKey(new DefaultHubConnection(
                    defaultHubById.get(sourceHubId),
                    defaultHubById.get(destinationHubId)));
        }
    }
}
