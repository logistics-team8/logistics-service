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
@Table(name = "p_delivery_route_histories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryRouteHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "route_id", nullable = false, updatable = false)
    private UUID routeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_id", nullable = false)
    private Delivery delivery;

    @Column(name = "route_sequence", nullable = false)
    private Integer sequence;

    @Column(name = "departure_hub_id", nullable = false)
    private UUID departureHubId;

    @Column(name = "arrival_hub_id", nullable = false)
    private UUID arrivalHubId;

    @Column(name = "estimated_distance_km", nullable = false)
    private BigDecimal estimatedDistanceKm;

    @Column(name = "estimated_duration_minutes", nullable = false)
    private Integer estimatedDurationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RouteStatus status;

    @Column(name = "hub_delivery_manager_id", nullable = false)
    private UUID hubDeliveryManagerId;
}
