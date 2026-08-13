package com.logistics.deliveryservice.domain.model;

import com.logistics.deliveryservice.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Delivery에 속한 하나의 허브 간 이동 구간과 계획 정보를 기록한다.
 */
@Getter
@Entity
@Table(
        name = "p_delivery_route_histories",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_delivery_route_histories_delivery_sequence",
                columnNames = {"delivery_id", "sequence"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryRouteHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID routeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_id", nullable = false)
    private Delivery delivery;

    @Column(name = "sequence", nullable = false)
    private Integer sequence;

    @Column(name = "departure_hub_id", nullable = false)
    private UUID departureHubId;

    @Column(name = "arrival_hub_id", nullable = false)
    private UUID arrivalHubId;

    @Column(name = "estimated_distance")
    private BigDecimal estimatedDistanceKm;

    @Column(name = "estimated_duration")
    private Integer estimatedDurationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private RouteStatus status;

    @Column(name = "hub_delivery_manager_id")
    private UUID hubDeliveryManagerId;

    /**
     * Hub 계획의 한 구간을 WAITING 상태의 배송 경로 이력으로 생성한다.
     */
    static DeliveryRouteHistory create(Delivery delivery, DeliveryPlan.Route routePlan) {
        DeliveryRouteHistory routeHistory = new DeliveryRouteHistory();
        routeHistory.delivery = delivery;
        routeHistory.sequence = routePlan.sequence();
        routeHistory.departureHubId = routePlan.departureHubId();
        routeHistory.arrivalHubId = routePlan.arrivalHubId();
        routeHistory.estimatedDistanceKm = routePlan.estimatedDistanceKm();
        routeHistory.estimatedDurationMinutes = routePlan.estimatedDurationMinutes();
        routeHistory.status = RouteStatus.WAITING;
        routeHistory.hubDeliveryManagerId = routePlan.hubDeliveryManagerId();
        return routeHistory;
    }
}
