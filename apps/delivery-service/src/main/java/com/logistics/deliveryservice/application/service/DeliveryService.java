package com.logistics.deliveryservice.application.service;

import com.logistics.deliveryservice.application.command.DeliveryCreateCommand;
import com.logistics.deliveryservice.application.dto.DeliveryCreateResponse;
import com.logistics.deliveryservice.application.dto.DeliveryCreateResult;
import com.logistics.deliveryservice.application.dto.DeliveryDetailResponse;
import com.logistics.deliveryservice.application.dto.DeliveryGetByOrderResponse;
import com.logistics.deliveryservice.application.dto.DeliveryRouteHistoryResponse;
import com.logistics.deliveryservice.application.dto.DeliveryRouteStatusUpdateResponse;
import com.logistics.deliveryservice.application.dto.DeliverySearchResponse;
import com.logistics.deliveryservice.application.dto.DeliveryStatusUpdateResponse;
import com.logistics.common.response.PageableUtil;
import com.logistics.common.security.principal.CustomUserDetails;
import com.logistics.deliveryservice.domain.exception.DeliveryErrorCode;
import com.logistics.deliveryservice.domain.exception.DeliveryException;
import com.logistics.deliveryservice.domain.model.Delivery;
import com.logistics.deliveryservice.domain.model.DeliveryPlan;
import com.logistics.deliveryservice.domain.model.DeliveryRouteHistory;
import com.logistics.deliveryservice.domain.port.HubDeliveryPlanProvider;
import com.logistics.deliveryservice.domain.repository.DeliveryRepository;
import com.logistics.deliveryservice.domain.repository.DeliveryRouteHistoryRepository;
import com.logistics.deliveryservice.presentation.dto.DeliverySearchRequest;
import com.logistics.deliveryservice.presentation.dto.DeliveryRouteStatusUpdateRequest;
import com.logistics.deliveryservice.presentation.dto.DeliveryStatusUpdateRequest;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주문 기반 배송 생성과 order_id 멱등성 판정을 조정한다.
 */
@Service
@RequiredArgsConstructor
public class DeliveryService {

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "createdAt",
            "updatedAt"
    );

    private final DeliveryRepository deliveryRepository;
    private final DeliveryRouteHistoryRepository deliveryRouteHistoryRepository;
    private final HubDeliveryPlanProvider hubDeliveryPlanProvider;

    /**
     * 기존 배송을 먼저 확인하고, 신규 주문일 때만 Hub 계획 조회와 Aggregate 저장을 수행한다.
     */
    public DeliveryCreateResult create(DeliveryCreateCommand command) {
        // 동일 orderId 재요청에서는 Hub 조회와 저장을 반복하지 않도록 기존 배송부터 확인한다.
        Optional<Delivery> existingDelivery = deliveryRepository.findByOrderId(command.orderId());
        if (existingDelivery.isPresent()) {
            return resolveExisting(existingDelivery.get(), command);
        }

        // 신규 주문일 때만 Hub Service에서 담당자와 전체 허브 이동 계획을 조회한다.
        DeliveryPlan deliveryPlan = hubDeliveryPlanProvider.getDeliveryPlan(
                command.orderId(),
                command.departureHubId(),
                command.arrivalHubId()
        );
        // Hub 계획의 무결성을 검증하면서 Delivery와 모든 Route를 하나의 Aggregate로 생성한다.
        Delivery delivery = Delivery.create(
                command.orderId(),
                command.requesterId(),
                command.departureHubId(),
                command.arrivalHubId(),
                command.deliveryAddress(),
                command.receiverName(),
                command.receiverSlackId(),
                deliveryPlan
        );

        try {
            // Aggregate Root를 저장하면 Cascade 설정에 따라 소속 Route도 함께 저장된다.
            Delivery savedDelivery = deliveryRepository.save(delivery);
            return DeliveryCreateResult.created(DeliveryCreateResponse.from(savedDelivery));
        } catch (DataIntegrityViolationException exception) {
            // 조회와 저장 사이 다른 요청이 먼저 생성했다면 재조회하여 멱등 또는 충돌로 판정한다.
            return resolveConcurrentCreation(command, exception);
        }
    }

    private DeliveryCreateResult resolveConcurrentCreation(
            DeliveryCreateCommand command,
            DataIntegrityViolationException exception
    ) {
        // 경합 상대가 만든 배송이 없으면 order_id 외 제약 위반으로 보고 원본 예외를 유지한다.
        return deliveryRepository.findByOrderId(command.orderId())
                .map(delivery -> resolveExisting(delivery, command))
                .orElseThrow(() -> exception);
    }

    private DeliveryCreateResult resolveExisting(
            Delivery existingDelivery,
            DeliveryCreateCommand command
    ) {
        // 취소·삭제된 배송 또는 불변 요청값이 다른 배송은 같은 주문의 멱등 요청으로 볼 수 없다.
        if (existingDelivery.isRecreationBlocked()
                || !existingDelivery.hasSameImmutablePayload(
                        command.requesterId(),
                        command.departureHubId(),
                        command.arrivalHubId(),
                        command.deliveryAddress(),
                        command.receiverName(),
                        command.receiverSlackId()
                )) {
            throw new DeliveryException(DeliveryErrorCode.DUPLICATE_ORDER_DELIVERY);
        }

        // 불변 요청값까지 같으면 중복 생성하지 않고 기존 배송을 정상 응답으로 반환한다.
        return DeliveryCreateResult.existing(DeliveryCreateResponse.from(existingDelivery));
    }

    /**
     * 논리 삭제되지 않은 배송을 주문 ID로 조회해 Order Service 응답으로 변환한다.
     */
    public DeliveryGetByOrderResponse getByOrderId(UUID orderId) {
        Delivery delivery = deliveryRepository.findActiveByOrderId(orderId)
                .orElseThrow(() -> new DeliveryException(DeliveryErrorCode.DELIVERY_NOT_FOUND));

        return DeliveryGetByOrderResponse.from(delivery);
    }

    /**
     * 로그인 사용자의 역할 범위를 확인한 뒤 활성 배송 한 건을 반환한다.
     */
    @Transactional(readOnly = true)
    public DeliveryDetailResponse getByDeliveryId(
            UUID deliveryId,
            CustomUserDetails userDetails
    ) {
        Delivery delivery = deliveryRepository.findActiveByDeliveryId(deliveryId)
                .orElseThrow(() -> new DeliveryException(DeliveryErrorCode.DELIVERY_NOT_FOUND));

        validateDetailReadPermission(delivery, userDetails);
        return DeliveryDetailResponse.from(delivery);
    }

    /**
     * 로그인 사용자의 역할 범위를 확인한 뒤 활성 배송의 경로 이력을 순서대로 반환한다.
     */
    @Transactional(readOnly = true)
    public List<DeliveryRouteHistoryResponse> getRoutesByDeliveryId(
            UUID deliveryId,
            CustomUserDetails userDetails
    ) {
        Delivery delivery = deliveryRepository.findActiveByDeliveryId(deliveryId)
                .orElseThrow(() -> new DeliveryException(DeliveryErrorCode.DELIVERY_NOT_FOUND));

        validateDetailReadPermission(delivery, userDetails);
        return deliveryRouteHistoryRepository.findActiveByDeliveryOrderBySequence(delivery).stream()
                .map(DeliveryRouteHistoryResponse::from)
                .toList();
    }

    /**
     * 로그인 사용자의 역할 범위 안에서 활성 배송을 검색한다.
     */
    @Transactional(readOnly = true)
    public Page<DeliverySearchResponse> search(
            DeliverySearchRequest request,
            Pageable pageable,
            CustomUserDetails userDetails
    ) {
        UUID hubId = null;
        UUID deliveryManagerId = null;

        if ("HUB_MANAGER".equals(userDetails.getRole())) {
            hubId = userDetails.getHubId();
            if (hubId == null) {
                throw new AccessDeniedException("허브 매니저의 담당 허브 정보가 없습니다.");
            }
        }

        if ("DELIVERY_MANAGER".equals(userDetails.getRole())) {
            deliveryManagerId = userDetails.getId();
        }

        Pageable normalizedPageable = PageableUtil.normalize(
                pageable,
                ALLOWED_SORT_PROPERTIES
        );

        return deliveryRepository.search(
                request.status(),
                request.orderId(),
                hubId,
                deliveryManagerId,
                normalizedPageable
        ).map(DeliverySearchResponse::from);
    }

    /**
     * 활성 배송을 권한 범위 안에서 논리 삭제
     */
    @Transactional
    public void delete(UUID deliveryId, CustomUserDetails userDetails) {
        Delivery delivery = deliveryRepository.findActiveByDeliveryId(deliveryId)
                .orElseThrow(() -> new DeliveryException(DeliveryErrorCode.DELIVERY_NOT_FOUND));

        validateDeletePermission(delivery, userDetails);
        delivery.delete(userDetails.getId());
        deliveryRepository.save(delivery);
    }

    /**
     * 활성 배송의 권한 범위를 확인한 뒤 요청한 상태로 변경한다.
     */
    @Transactional
    public DeliveryStatusUpdateResponse updateStatus(
            UUID deliveryId,
            DeliveryStatusUpdateRequest request,
            CustomUserDetails userDetails
    ) {
        Delivery delivery = deliveryRepository.findActiveByDeliveryId(deliveryId)
                .orElseThrow(() -> new DeliveryException(DeliveryErrorCode.DELIVERY_NOT_FOUND));

        validateDetailReadPermission(delivery, userDetails);
        delivery.changeStatus(request.status());
        Delivery savedDelivery = deliveryRepository.save(delivery);
        return DeliveryStatusUpdateResponse.from(savedDelivery);
    }

    /**
     * 활성 배송에 속한 활성 경로의 권한 범위를 확인한 뒤 요청한 상태로 변경한다.
     */
    @Transactional
    public DeliveryRouteStatusUpdateResponse updateRouteStatus(
            UUID deliveryId,
            UUID routeId,
            DeliveryRouteStatusUpdateRequest request,
            CustomUserDetails userDetails
    ) {
        Delivery delivery = deliveryRepository.findActiveByDeliveryId(deliveryId)
                .orElseThrow(() -> new DeliveryException(DeliveryErrorCode.DELIVERY_NOT_FOUND));

        validateDetailReadPermission(delivery, userDetails);

        DeliveryRouteHistory routeHistory = deliveryRouteHistoryRepository
                .findActiveByRouteIdAndDelivery(routeId, delivery)
                .orElseThrow(() -> new DeliveryException(DeliveryErrorCode.ROUTE_NOT_FOUND));

        routeHistory.changeStatus(request.status());
        DeliveryRouteHistory savedRouteHistory = deliveryRouteHistoryRepository.save(routeHistory);
        return DeliveryRouteStatusUpdateResponse.from(savedRouteHistory);
    }

    private void validateDetailReadPermission(
            Delivery delivery,
            CustomUserDetails userDetails
    ) {
        if ("MASTER".equals(userDetails.getRole())) {
            return;
        }

        if ("HUB_MANAGER".equals(userDetails.getRole())) {
            UUID hubId = userDetails.getHubId();
            if (hubId != null && isHubRelatedDelivery(delivery, hubId)) {
                return;
            }
            throw new AccessDeniedException("허브 매니저는 담당 허브의 배송만 조회할 수 있습니다.");
        }

        if ("DELIVERY_MANAGER".equals(userDetails.getRole())) {
            if (isAssignedDelivery(delivery, userDetails.getId())) {
                return;
            }
            throw new AccessDeniedException("배송 담당자는 자신에게 배정된 배송만 조회할 수 있습니다.");
        }
    }

    private boolean isHubRelatedDelivery(Delivery delivery, UUID hubId) {
        return hubId.equals(delivery.getDepartureHubId())
                || hubId.equals(delivery.getArrivalHubId())
                || delivery.getRouteHistories().stream()
                .filter(routeHistory -> !routeHistory.isDeleted())
                .anyMatch(routeHistory -> hubId.equals(routeHistory.getDepartureHubId())
                        || hubId.equals(routeHistory.getArrivalHubId()));
    }

    private boolean isAssignedDelivery(Delivery delivery, UUID managerUserId) {
        return managerUserId.equals(delivery.getDeliveryManagerId())
                || delivery.getRouteHistories().stream()
                .filter(routeHistory -> !routeHistory.isDeleted())
                .anyMatch(routeHistory -> managerUserId.equals(
                        routeHistory.getHubDeliveryManagerId()
                ));
    }

    private void validateDeletePermission(
            Delivery delivery,
            CustomUserDetails userDetails
    ) {
        if ("MASTER".equals(userDetails.getRole())) {
            return;
        }

        if ("HUB_MANAGER".equals(userDetails.getRole())
                && userDetails.getHubId() != null
                && isHubRelatedDelivery(delivery, userDetails.getHubId())) {
            return;
        }

        throw new AccessDeniedException("배송 삭제 권한이 없습니다.");
    }
}
