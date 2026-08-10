package com.logistics.hubservice.application.hubroute.initialization;

import com.logistics.hubservice.domain.hub.Hub;
import com.logistics.hubservice.domain.hub.HubRepository;
import com.logistics.hubservice.domain.hubroute.HubRoute;
import com.logistics.hubservice.domain.hubroute.HubRouteRepository;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;

public class HubRouteDefaultDataService {

    private final HubRepository hubRepository;
    private final HubRouteRepository hubRouteRepository;
    private final HubLocationProvider locationProvider;
    private final RouteMetricProvider metricProvider;

    public HubRouteDefaultDataService(
            HubRepository hubRepository,
            HubRouteRepository hubRouteRepository,
            HubLocationProvider locationProvider,
            RouteMetricProvider metricProvider) {
        this.hubRepository = hubRepository;
        this.hubRouteRepository = hubRouteRepository;
        this.locationProvider = locationProvider;
        this.metricProvider = metricProvider;
    }

    @Transactional
    @CacheEvict(cacheNames = "hubRoutePath", allEntries = true)
    public HubRouteDefaultDataResult initialize() {
        HubPlan hubPlan = planHubs();
        List<HubRoute> hubRoutesToCreate = planHubRoutes(hubPlan.hubsByDefinition());

        hubPlan.hubsToCreate().forEach(hubRepository::save);
        hubRoutesToCreate.forEach(hubRouteRepository::save);

        return new HubRouteDefaultDataResult(
                hubPlan.hubsToCreate().size(),
                hubRoutesToCreate.size());
    }

    private HubPlan planHubs() {
        Map<DefaultHub, Hub> hubsByDefinition = new EnumMap<>(DefaultHub.class);
        List<Hub> hubsToCreate = new ArrayList<>();

        for (DefaultHub defaultHub : DefaultHub.values()) {
            Optional<Hub> existingHub =
                    hubRepository.findByIdAndDeletedAtIsNull(defaultHub.hubId());
            Hub hub = existingHub.orElseGet(() -> createHub(defaultHub));
            hubsByDefinition.put(defaultHub, hub);
            if (existingHub.isEmpty()) {
                hubsToCreate.add(hub);
            }
        }
        return new HubPlan(hubsByDefinition, List.copyOf(hubsToCreate));
    }

    private Hub createHub(DefaultHub defaultHub) {
        HubCoordinates coordinates = locationProvider.geocode(defaultHub.address());
        return Hub.createDefault(
                defaultHub.hubId(),
                defaultHub.hubName(),
                defaultHub.address(),
                coordinates.latitude(),
                coordinates.longitude());
    }

    private List<HubRoute> planHubRoutes(Map<DefaultHub, Hub> hubsByDefinition) {
        List<HubRoute> hubRoutesToCreate = new ArrayList<>();
        for (DefaultHubConnection connection : DefaultHubRouteTopology.directedConnections()) {
            Hub source = hubsByDefinition.get(connection.source());
            Hub destination = hubsByDefinition.get(connection.destination());
            if (hubRouteRepository.existsBySourceHubIdAndDestinationHubIdAndDeletedAtIsNull(
                    source.getId(), destination.getId())) {
                continue;
            }

            RouteMetric metric = metricProvider.getMetric(
                    coordinatesOf(source),
                    coordinatesOf(destination));
            hubRoutesToCreate.add(HubRoute.createDefault(
                    source.getId(),
                    destination.getId(),
                    metric.distanceMeters(),
                    metric.durationSeconds()));
        }
        return List.copyOf(hubRoutesToCreate);
    }

    private HubCoordinates coordinatesOf(Hub hub) {
        return new HubCoordinates(hub.getLatitude(), hub.getLongitude());
    }

    private record HubPlan(Map<DefaultHub, Hub> hubsByDefinition, List<Hub> hubsToCreate) {
    }
}
