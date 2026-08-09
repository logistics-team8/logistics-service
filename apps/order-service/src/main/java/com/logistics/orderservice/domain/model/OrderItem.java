package com.logistics.orderservice.domain.model;

import com.logistics.common.exception.BusinessException;
import com.logistics.orderservice.error.OrderErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import java.util.UUID;

@Entity
@Table(name = "p_order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_item_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    //상품이름, 공급업체 id는 일단 null으로 유지하고 delivery와 product 연동 시 NOT NULL 변경
    @Column(name = "product_name", length = 30)
    private String productName;

    @Column(name = "supplier_company_id")
    private UUID supplierCompanyId;

    /**
     * 공급업체가 소속된 출발 허브
     *
     * Product/Company Service 연동 후 저장한다.
     */
    @Column(name = "departure_hub_id")
    private UUID departureHubId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OrderItemStatus status;

    @Column(name = "canceled_by")
    private UUID canceledBy;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;



    private OrderItem(
            Order order,
            UUID productId,
            Integer quantity
    ) {
        this.order = order;
        this.productId = productId;
        this.quantity = quantity;
        this.status = OrderItemStatus.ACTIVE;
    }

    static OrderItem create(
            Order order,
            UUID productId,
            Integer quantity
    ) {
        validateQuantity(quantity);
        return new OrderItem(
                order,
                productId,
                quantity
        );
    }


    public void delete(UUID deleteBy){
        softDelete(deleteBy, LocalDateTime.now());
    }


    public void cancel(UUID canceledBy, LocalDateTime canceledAt) {
        if(this.status == OrderItemStatus.CANCELED) {
            throw new BusinessException(
                    OrderErrorCode.ORDER_ITEM_ALREADY_CANCELED
            );
        }

        this.status = OrderItemStatus.CANCELED;
        this.canceledBy = canceledBy;
        this.canceledAt = canceledAt;
    }

    public boolean isCanceled() {
        return status == OrderItemStatus.CANCELED;
    }

    private static void validateQuantity(Integer quantity){
        if(quantity == null || quantity < 1) {
            throw new BusinessException(OrderErrorCode.INVALID_ORDER_QUANTITY);
        }
    }



}
