package com.logistics.deliveryservice.infrastructure.persistence;

import com.logistics.deliveryservice.domain.model.Delivery;
import com.logistics.deliveryservice.domain.model.DeliveryStatus;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface DeliveryJpaRepository extends JpaRepository<Delivery, UUID> {

    @EntityGraph(attributePaths = "routeHistories")
    Optional<Delivery> findByOrderId(UUID orderId);

    @EntityGraph(attributePaths = "routeHistories")
    Optional<Delivery> findByOrderIdAndDeletedAtIsNull(UUID orderId);

    // 특정 배송 담당자에게 현재 배송 작업이 할당되어있는지 확인
    @Query("""
            select case when count(delivery) > 0 then true else false end
            from Delivery delivery
            left join delivery.routeHistories routeHistory
            where delivery.deletedAt is null
              and delivery.status in :deliveryStatuses
              and (
                    delivery.companyDeliveryManagerId = :managerUserId
                    or (
                        routeHistory.hubDeliveryManagerId = :managerUserId
                        and routeHistory.deletedAt is null
                    )
              )
            """)
    boolean existsActiveManagerAssignment(
            @Param("managerUserId") UUID managerUserId,
            @Param("deliveryStatuses") Collection<DeliveryStatus> deliveryStatuses
    );
}
