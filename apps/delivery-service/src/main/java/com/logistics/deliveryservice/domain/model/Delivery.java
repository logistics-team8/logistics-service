package com.logistics.deliveryservice.domain.model;

import com.logistics.deliveryservice.domain.common.BaseEntity;
import com.logistics.deliveryservice.domain.exception.DeliveryErrorCode;
import com.logistics.deliveryservice.domain.exception.DeliveryException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문 한 건의 전체 배송 과정과 허브 간 경로 이력을 관리하는 Aggregate Root다.
 */
@Getter
@Entity
@Table(
        name = "p_deliveries",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_deliveries_order_id",
                columnNames = "order_id"
        ),
        indexes = {
                @Index(name = "idx_deliveries_status", columnList = "status"),
                @Index(name = "idx_deliveries_departure_hub_id", columnList = "departure_hub_id"),
                @Index(name = "idx_deliveries_arrival_hub_id", columnList = "arrival_hub_id"),
                @Index(
                        name = "idx_deliveries_delivery_manager_id",
                        columnList = "delivery_manager_id"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Delivery extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID deliveryId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "requester_id", nullable = false)
    private UUID requesterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private DeliveryStatus status;

    @Column(name = "departure_hub_id", nullable = false)
    private UUID departureHubId;

    @Column(name = "arrival_hub_id", nullable = false)
    private UUID arrivalHubId;

    @Column(name = "delivery_address", nullable = false)
    private String deliveryAddress;

    @Column(name = "receiver_name", nullable = false, length = 100)
    private String receiverName;

    @Column(name = "receiver_slack_id", length = 100)
    private String receiverSlackId;

    @Column(name = "delivery_manager_id")
    private UUID deliveryManagerId;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Getter(AccessLevel.NONE)
    @OneToMany(
            mappedBy = "delivery",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE}
    )
    private final List<DeliveryRouteHistory> routeHistories = new ArrayList<>();

    /**
     * 주문 정보와 검증된 Hub 계획으로 Delivery Aggregate 전체를 생성한다.
     */
    public static Delivery create(
            UUID orderId,
            UUID requesterId,
            UUID departureHubId,
            UUID arrivalHubId,
            String deliveryAddress,
            String receiverName,
            String receiverSlackId,
            DeliveryPlan deliveryPlan
    ) {
        // 외부 Hub Service가 준 계획을 그대로 신뢰하지 않고 Aggregate 생성 전에 전체 규칙을 검증한다.
        validateDeliveryPlan(departureHubId, arrivalHubId, deliveryPlan);

        Delivery delivery = new Delivery();
        delivery.orderId = orderId;
        delivery.requesterId = requesterId;
        delivery.departureHubId = departureHubId;
        delivery.arrivalHubId = arrivalHubId;
        delivery.deliveryAddress = deliveryAddress;
        delivery.receiverName = receiverName;
        delivery.receiverSlackId = receiverSlackId;
        delivery.deliveryManagerId = deliveryPlan.companyDeliveryManagerId();
        // 같은 허브 배송은 이동 경로가 없으므로 도착 허브에 도착한 상태에서 업체 배송을 기다린다.
        delivery.status = departureHubId.equals(arrivalHubId)
                ? DeliveryStatus.HUB_ARRIVED
                : DeliveryStatus.HUB_WAIT;

        // 검증된 각 Hub 이동 구간을 Delivery에 소속된 Route 이력으로 생성한다.
        deliveryPlan.routes().stream()
                .map(routePlan -> DeliveryRouteHistory.create(delivery, routePlan))
                .forEach(delivery.routeHistories::add);
        return delivery;
    }

    /**
     * Aggregate 밖에서 경로 이력 컬렉션을 직접 변경하지 못하도록 읽기 전용으로 반환한다.
     */
    public List<DeliveryRouteHistory> getRouteHistories() {
        return Collections.unmodifiableList(routeHistories);
    }

    /**
     * 동일 주문의 재요청이 최초 생성 요청과 같은 불변 정보를 담고 있는지 확인한다.
     */
    public boolean hasSameImmutablePayload(
            UUID requesterId,
            UUID departureHubId,
            UUID arrivalHubId,
            String deliveryAddress,
            String receiverName,
            String receiverSlackId
    ) {
        return Objects.equals(this.requesterId, requesterId)
                && Objects.equals(this.departureHubId, departureHubId)
                && Objects.equals(this.arrivalHubId, arrivalHubId)
                && Objects.equals(this.deliveryAddress, deliveryAddress)
                && Objects.equals(this.receiverName, receiverName)
                && Objects.equals(this.receiverSlackId, receiverSlackId);
    }

    /**
     * 취소되었거나 논리 삭제된 배송은 같은 주문으로 다시 생성할 수 없다.
     */
    public boolean isRecreationBlocked() {
        return status == DeliveryStatus.CANCELED || isDeleted();
    }

    // soft delete
    public void delete(UUID deletedBy) {
        markDeleted(deletedBy);
    }

    private static void validateDeliveryPlan(
            UUID departureHubId,
            UUID arrivalHubId,
            DeliveryPlan deliveryPlan
    ) {
        // 배송과 계획을 구성하는 최상위 필수값이 하나라도 없으면 전체 계획을 거부한다.
        // 배송·경로 담당자는 배송 생성 이후의 배정 단계에서 설정하므로 이 시점에는 없을 수 있다.
        if (departureHubId == null
                || arrivalHubId == null
                || deliveryPlan == null
                || deliveryPlan.routes() == null) {
            throw invalidDeliveryPlan();
        }

        List<DeliveryPlan.Route> routes = deliveryPlan.routes();
        boolean sameHubDelivery = departureHubId.equals(arrivalHubId);
        if (sameHubDelivery) {
            // 출발·도착 허브가 같으면 허브 간 이동이 없으므로 Route가 존재해서는 안 된다.
            if (!routes.isEmpty()) {
                throw invalidDeliveryPlan();
            }
            return;
        }

        // 서로 다른 허브 사이의 배송에는 최소 한 개 이상의 이동 구간이 필요하다.
        if (routes.isEmpty()) {
            throw invalidDeliveryPlan();
        }

        for (int index = 0; index < routes.size(); index++) {
            DeliveryPlan.Route route = routes.get(index);
            validateRoute(route, index + 1);

            // 첫 Route는 배송 출발 허브에서 시작해야 한다.
            if (index == 0 && !departureHubId.equals(route.departureHubId())) {
                throw invalidDeliveryPlan();
            }
            if (index > 0) {
                DeliveryPlan.Route previousRoute = routes.get(index - 1);
                // 앞 Route의 도착 허브와 다음 Route의 출발 허브가 이어져야 한다.
                if (!previousRoute.arrivalHubId().equals(route.departureHubId())) {
                    throw invalidDeliveryPlan();
                }
            }
        }

        // 마지막 Route가 배송의 최종 도착 허브에서 끝나는지 확인한다.
        DeliveryPlan.Route lastRoute = routes.get(routes.size() - 1);
        if (!arrivalHubId.equals(lastRoute.arrivalHubId())) {
            throw invalidDeliveryPlan();
        }
    }

    private static void validateRoute(DeliveryPlan.Route route, int expectedSequence) {
        // Route 순서는 1부터 연속되어야 하며 거리·시간은 음수가 될 수 없다.
        if (route == null
                || route.sequence() == null
                || route.sequence() != expectedSequence
                || route.departureHubId() == null
                || route.arrivalHubId() == null
                || route.estimatedDistanceKm() == null
                || route.estimatedDistanceKm().signum() < 0
                || route.estimatedDurationMinutes() == null
                || route.estimatedDurationMinutes() < 0) {
            throw invalidDeliveryPlan();
        }
    }

    private static DeliveryException invalidDeliveryPlan() {
        return new DeliveryException(DeliveryErrorCode.INVALID_HUB_DELIVERY_PLAN);
    }
}
