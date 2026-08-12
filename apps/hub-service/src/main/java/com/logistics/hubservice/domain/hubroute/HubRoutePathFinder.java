package com.logistics.hubservice.domain.hubroute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class HubRoutePathFinder {

    private static final Comparator<HubRoute> ROUTE_ORDER = Comparator
            .comparing(HubRoute::getDestinationHubId)
            .thenComparing(
                    HubRoute::getId,
                    Comparator.nullsLast(Comparator.naturalOrder()));
    private static final Comparator<PathState> PATH_STATE_ORDER = Comparator
            .comparingLong(PathState::durationSeconds)
            .thenComparingLong(PathState::distanceMeters)
            .thenComparing(PathState::hubId);

    public Optional<HubRoutePath> findShortestPath(
            List<HubRoute> activeRoutes,
            UUID sourceHubId,
            UUID destinationHubId) {
        if (sourceHubId.equals(destinationHubId)) {
            return Optional.of(HubRoutePath.empty());
        }

        Map<UUID, List<HubRoute>> graph = buildGraph(activeRoutes);
        Map<UUID, PathCost> bestCosts = new HashMap<>();
        Map<UUID, HubRoute> previousRoutes = new HashMap<>();
        PriorityQueue<PathState> candidates = new PriorityQueue<>(PATH_STATE_ORDER);

        PathCost initialCost = new PathCost(0L, 0L);
        bestCosts.put(sourceHubId, initialCost);
        candidates.add(new PathState(sourceHubId, 0L, 0L));

        while (!candidates.isEmpty()) {
            PathState current = candidates.poll();
            PathCost currentBest = bestCosts.get(current.hubId());
            if (currentBest == null || !currentBest.matches(current)) {
                continue;
            }
            if (current.hubId().equals(destinationHubId)) {
                break;
            }

            for (HubRoute route : graph.getOrDefault(current.hubId(), List.of())) {
                if (wouldOverflow(current, route)) {
                    continue;
                }

                PathCost candidateCost = new PathCost(
                        current.distanceMeters() + route.getDistanceMeters(),
                        current.durationSeconds() + route.getDurationSeconds());
                UUID nextHubId = route.getDestinationHubId();
                PathCost nextBest = bestCosts.get(nextHubId);
                if (nextBest != null && candidateCost.compareTo(nextBest) >= 0) {
                    continue;
                }

                bestCosts.put(nextHubId, candidateCost);
                previousRoutes.put(nextHubId, route);
                candidates.add(new PathState(
                        nextHubId,
                        candidateCost.distanceMeters(),
                        candidateCost.durationSeconds()));
            }
        }

        PathCost totalCost = bestCosts.get(destinationHubId);
        if (totalCost == null) {
            return Optional.empty();
        }

        List<HubRoute> segments = reconstructPath(
                previousRoutes,
                sourceHubId,
                destinationHubId);
        if (segments.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new HubRoutePath(
                segments,
                totalCost.distanceMeters(),
                totalCost.durationSeconds()));
    }

    private Map<UUID, List<HubRoute>> buildGraph(List<HubRoute> activeRoutes) {
        Map<UUID, List<HubRoute>> graph = new HashMap<>();
        for (HubRoute route : activeRoutes) {
            graph.computeIfAbsent(route.getSourceHubId(), ignored -> new ArrayList<>())
                    .add(route);
        }
        graph.values().forEach(routes -> routes.sort(ROUTE_ORDER));
        return graph;
    }

    private boolean wouldOverflow(PathState current, HubRoute route) {
        return Long.MAX_VALUE - current.distanceMeters() < route.getDistanceMeters()
                || Long.MAX_VALUE - current.durationSeconds() < route.getDurationSeconds();
    }

    private List<HubRoute> reconstructPath(
            Map<UUID, HubRoute> previousRoutes,
            UUID sourceHubId,
            UUID destinationHubId) {
        List<HubRoute> reversedPath = new ArrayList<>();
        UUID currentHubId = destinationHubId;

        while (!currentHubId.equals(sourceHubId)) {
            HubRoute previousRoute = previousRoutes.get(currentHubId);
            if (previousRoute == null) {
                return List.of();
            }
            reversedPath.add(previousRoute);
            currentHubId = previousRoute.getSourceHubId();
        }

        Collections.reverse(reversedPath);
        return reversedPath;
    }

    private record PathState(
            UUID hubId,
            long distanceMeters,
            long durationSeconds
    ) {
    }

    private record PathCost(
            long distanceMeters,
            long durationSeconds
    ) implements Comparable<PathCost> {

        @Override
        public int compareTo(PathCost other) {
            int durationComparison = Long.compare(durationSeconds, other.durationSeconds);
            if (durationComparison != 0) {
                return durationComparison;
            }
            return Long.compare(distanceMeters, other.distanceMeters);
        }

        private boolean matches(PathState state) {
            return distanceMeters == state.distanceMeters()
                    && durationSeconds == state.durationSeconds();
        }
    }
}
