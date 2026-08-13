package com.logistics.orderservice.infrastructure.persistence;

import com.logistics.orderservice.domain.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface OrderJpaRepository extends JpaRepository<Order, UUID> {

    @EntityGraph(attributePaths = "orderItems")
    Optional<Order> findByIdAndDeletedAtIsNull(UUID orderId);

    Page<Order> findAllByDeletedAtIsNull(Pageable pageable);



    Optional<Order> findByIdAndRequesterIdAndDeletedAtIsNull(UUID orderId, UUID userId);

    Page<Order> findAllByRequesterIdAndDeletedAtIsNull(UUID userId, Pageable pageable);


    /**
     * 로그인한 HUB_MANAGER의 허브가
     * 주문의 도착 허브 또는 주문상품의 출발 허브인 주문 조회
     */
    @Query(
            value = """
                    SELECT o
                    FROM Order o
                    WHERE o.deletedAt IS NULL
                      AND (
                          o.destinationHubId = :hubId
                          OR EXISTS (
                              SELECT oi.id
                              FROM OrderItem oi
                              WHERE oi.order = o
                                AND oi.deletedAt IS NULL
                                AND oi.departureHubId = :hubId
                          )
                      )
                    """,
            countQuery = """
                    SELECT COUNT(o.id)
                    FROM Order o
                    WHERE o.deletedAt IS NULL
                      AND (
                          o.destinationHubId = :hubId
                          OR EXISTS (
                              SELECT oi.id
                              FROM OrderItem oi
                              WHERE oi.order = o
                                AND oi.deletedAt IS NULL
                                AND oi.departureHubId = :hubId
                          )
                      )
                    """
    )
    Page<Order> findAllByManagedHubIdAndDeletedAtIsNull(
            @Param("hubId") UUID hubId, Pageable pageable
    );

    Optional<Order> findByRequesterIdAndIdempotencyKey(UUID requesterId, String idempotencyKey);

}
