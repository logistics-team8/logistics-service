package com.logistics.orderservice.domain.model;

import com.logistics.common.exception.BusinessException;
import com.logistics.orderservice.config.BaseEntity;
import com.logistics.orderservice.error.OrderErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Entity
@Table(name = "p_orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_id")
    private UUID id;

    @Column(
            name = "order_number",
            nullable = false,
            unique = true,
            length = 50
    )
    private String orderNumber;

    @Column(name = "requester_id", nullable = false)
    private UUID requesterId;

    @Column(name = "receiver_company_id", nullable = false)
    private UUID receiverCompanyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OrderStatus status;

    @Column(name = "request_message", length = 500)
    private String requestMessage;

    @Column(name = "requested_delivery_at")
    private LocalDateTime requestedDeliveryAt;

    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private final List<OrderItem> orderItems =
            new ArrayList<>();

    @Column(name = "canceled_by")
    private UUID canceledBy;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;


    private Order(
            String orderNumber,
            UUID requesterId,
            UUID receiverCompanyId,
            String requestMessage,
            LocalDateTime requestedDeliveryAt
    ) {
        this.orderNumber = orderNumber;
        this.requesterId = requesterId;
        this.receiverCompanyId = receiverCompanyId;
        this.requestMessage = requestMessage;
        this.requestedDeliveryAt = requestedDeliveryAt;
        this.status = OrderStatus.PENDING;
    }

    public static Order create(
            String orderNumber,
            UUID requesterId,
            UUID receiverCompanyId,
            String requestMessage,
            LocalDateTime requestedDeliveryAt
    ) {
        return new Order(
                orderNumber,
                requesterId,
                receiverCompanyId,
                requestMessage,
                requestedDeliveryAt
        );
    }


    public void addOrderItem(UUID productId, Integer quantity){
        validateDuplicateProduct(productId);

        OrderItem orderItem =
                OrderItem.create(this, productId, quantity);
        this.orderItems.add(orderItem);
    }


    private void validateDuplicateProduct(UUID productId){
        boolean duplicatedOrderItem = orderItems.stream()
                .anyMatch(orderItem ->
                        orderItem.getProductId().equals(productId)
                );

        //하나의 주문안에 같은 상품ID 중복 방지
        if(duplicatedOrderItem){
            throw new BusinessException(
                    OrderErrorCode.DUPLICATE_ORDER_PRODUCT
            );
        }
    }

}