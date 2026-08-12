package com.logistics.deliveryservice.application.service;

import com.logistics.deliveryservice.domain.exception.DeliveryErrorCode;
import com.logistics.deliveryservice.domain.exception.DeliveryException;
import com.logistics.deliveryservice.domain.model.DeliveryManager;
import com.logistics.deliveryservice.domain.model.DeliveryManagerAssignmentCursor;
import com.logistics.deliveryservice.domain.model.DeliveryManagerAssignmentGroup;
import com.logistics.deliveryservice.domain.model.DeliveryPlan;
import com.logistics.deliveryservice.domain.repository.DeliveryManagerAssignmentCursorRepository;
import com.logistics.deliveryservice.domain.repository.DeliveryManagerRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 배송 계획의 업체/허브 담당자를 그룹 Cursor 기준으로 round-robin 배정한다.
 */
@Service
@RequiredArgsConstructor
public class DeliveryManagerAssignmentService {

    private final DeliveryManagerAssignmentCursorRepository cursorRepository;
    private final DeliveryManagerRepository deliveryManagerRepository;

    /**
     * 도착 허브의 업체 담당자 1명과, 경로가 있으면 전역 허브 담당자를 구간마다 배정한다.
     */
    public DeliveryPlan assignManagers(DeliveryPlan plan, UUID arrivalHubId) {
        if (plan == null) {
            throw new DeliveryException(DeliveryErrorCode.INVALID_HUB_DELIVERY_PLAN);
        }

        UUID companyDeliveryManagerId = assignNextManager(
                DeliveryManagerAssignmentGroup.companyDelivery(arrivalHubId)
        );

        List<DeliveryPlan.Route> routes = plan.routes();
        if (routes == null || routes.isEmpty()) {
            return new DeliveryPlan(companyDeliveryManagerId, routes);
        }

        DeliveryManagerAssignmentGroup hubGroup =
                DeliveryManagerAssignmentGroup.hubDelivery(arrivalHubId);
        DeliveryManagerAssignmentCursor hubCursor = cursorRepository.acquireForUpdate(hubGroup);
        List<DeliveryManager> hubManagers = deliveryManagerRepository.findActiveManagers(hubGroup);

        List<DeliveryPlan.Route> assignedRoutes = routes.stream()
                .map(route -> assignHubManager(route, hubCursor, hubManagers))
                .toList();
        return new DeliveryPlan(companyDeliveryManagerId, assignedRoutes);
    }

    private DeliveryPlan.Route assignHubManager(
            DeliveryPlan.Route route,
            DeliveryManagerAssignmentCursor hubCursor,
            List<DeliveryManager> hubManagers
    ) {
        if (route == null) {
            return null;
        }
        return new DeliveryPlan.Route(
                route.sequence(),
                route.departureHubId(),
                route.arrivalHubId(),
                route.estimatedDistanceKm(),
                route.estimatedDurationMinutes(),
                selectNextManagerId(hubCursor, hubManagers)
        );
    }

    private UUID assignNextManager(DeliveryManagerAssignmentGroup assignmentGroup) {
        DeliveryManagerAssignmentCursor cursor =
                cursorRepository.acquireForUpdate(assignmentGroup);
        List<DeliveryManager> activeManagers =
                deliveryManagerRepository.findActiveManagers(assignmentGroup);
        return selectNextManagerId(cursor, activeManagers);
    }

    private UUID selectNextManagerId(
            DeliveryManagerAssignmentCursor cursor,
            List<DeliveryManager> activeManagers
    ) {
        List<Integer> activeSequences = activeManagers.stream()
                .map(DeliveryManager::getDeliverySequence)
                .toList();
        int selectedSequence = cursor.assignNext(activeSequences);
        return activeManagers.stream()
                .filter(manager -> selectedSequence == manager.getDeliverySequence())
                .findFirst()
                .orElseThrow(() -> new DeliveryException(
                        DeliveryErrorCode.DELIVERY_MANAGER_UNAVAILABLE
                ))
                .getUserId();
    }
}
