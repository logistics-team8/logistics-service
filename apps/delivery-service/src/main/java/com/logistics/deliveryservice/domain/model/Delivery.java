package com.logistics.deliveryservice.domain.model;

import com.logistics.deliveryservice.domain.common.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문 한 건의 전체 배송 과정과 허브 간 경로 이력을 관리하는 Aggregate Root다.
 */
@Getter
@Entity
@Table(name = "p_deliveries")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Delivery extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "delivery_id", nullable = false, updatable = false)
    private UUID deliveryId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "requester_id", nullable = false)
    private UUID requesterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private DeliveryStatus status;

    @Column(name = "departure_hub_id", nullable = false)
    private UUID departureHubId;

    @Column(name = "arrival_hub_id", nullable = false)
    private UUID arrivalHubId;

    @Column(name = "delivery_address", nullable = false)
    private String deliveryAddress;

    @Column(name = "receiver_name", nullable = false)
    private String receiverName;

    @Column(name = "receiver_slack_id")
    private String receiverSlackId;

    @Column(name = "company_delivery_manager_id", nullable = false)
    private UUID companyDeliveryManagerId;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Getter(AccessLevel.NONE)
    @OneToMany(
            mappedBy = "delivery",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE}
    )
    private final List<DeliveryRouteHistory> routeHistories = new ArrayList<>();

    /**
     * Aggregate 밖에서 경로 이력 컬렉션을 직접 변경하지 못하도록 읽기 전용으로 반환한다.
     */
    public List<DeliveryRouteHistory> getRouteHistories() {
        return Collections.unmodifiableList(routeHistories);
    }
}
