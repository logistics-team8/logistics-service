package com.logistics.orderservice.domain.model;

import com.logistics.common.exception.BusinessException;
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

    /**
     * 수령업체의 소속 허브
     *
     * Company Service 연동 후 저장한다.
     */
    @Column(name = "destination_hub_id")
    private UUID destinationHubId;

    /**
     * 주문 생성 당시 수령업체 주소
     *
     * Company Service 연동 후 저장한다.
     */
    @Column(name = "delivery_address", length = 255)
    private String deliveryAddress;

    /**
     * 주문 생성 당시 수령인 이름
     *
     * User Service 연동 후 저장한다.
     */
    @Column(name = "receiver_name", length = 100)
    private String receiverName;

    /**
     * 주문 생성 당시 수령인 Slack ID
     *
     * User Service 연동 후 저장한다.
     */
    @Column(name = "receiver_slack_id", length = 100)
    private String receiverSlackId;


    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OrderStatus status;

    @Column(name = "request_message", length = 500)
    private String requestMessage;

    @Column(name = "requested_delivery_at")
    private LocalDateTime requestedDeliveryAt;

    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL
            //orphanRemoval = true
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
            LocalDateTime requestedDeliveryAt,
            LocalDateTime now
    ) {
        validateRequestedDeliveryAt(requestedDeliveryAt,now);

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


    public void update(
            String requestMessage,
            LocalDateTime requestedDeliveryAt,
            LocalDateTime now
    ){
        if(this.status != OrderStatus.PENDING){
            throw new BusinessException(
                    OrderErrorCode.ORDER_NOT_UPDATABLE
            );
        }
        if(requestMessage != null){
            this.requestMessage = requestMessage;
        }
        if(requestedDeliveryAt != null){
            validateRequestedDeliveryAt(requestedDeliveryAt,now);
            this.requestedDeliveryAt = requestedDeliveryAt;
        }
    }


    public void delete(UUID deleteBy){
        if(this.status != OrderStatus.CANCELED
                && this.status != OrderStatus.FAILED){
            throw new BusinessException(OrderErrorCode.ORDER_NOT_DELETABLE);
        }

        this.orderItems.forEach(
                orderItem -> orderItem.delete(deleteBy)
        );

        softDelete(deleteBy, LocalDateTime.now());
    }


    public void cancel(UUID canceledBy){
        validateCancelable();

        this.orderItems.stream()
                //제외 가능
                .filter(orderItem ->
                        orderItem.getDeletedAt() == null
                )
                .filter(orderItem ->
                        !orderItem.isCanceled()
                )
                .forEach(orderItem ->
                        orderItem.cancel(
                                canceledBy,
                                LocalDateTime.now()
                        )
                );
        this.status = OrderStatus.CANCELED;
        this.canceledBy = canceledBy;
        this.canceledAt = LocalDateTime.now();
    }


    public OrderItem cancelOrderItem(UUID orderItemId, UUID canceledBy) {
        validateCancelable();

        OrderItem orderItem = orderItems.stream()
                .filter(item -> item.getId().equals(orderItemId))
                .findFirst()
                .orElseThrow( () ->
                            new BusinessException(OrderErrorCode.ORDER_ITEM_NOT_FOUND)
                );

        orderItem.cancel(canceledBy, LocalDateTime.now());

        if(orderItems.stream().allMatch(OrderItem::isCanceled)){
            this.status = OrderStatus.CANCELED;
            this.canceledBy = canceledBy;
            this.canceledAt = LocalDateTime.now();
        }

        return orderItem;

    }





    //하나의 주문안에 같은 상품ID 중복 방지
    private void validateDuplicateProduct(UUID productId){
        boolean duplicatedOrderItem = orderItems.stream()
                .anyMatch(orderItem ->
                        orderItem.getProductId().equals(productId)
                );

        if(duplicatedOrderItem){
            throw new BusinessException(
                    OrderErrorCode.DUPLICATE_ORDER_PRODUCT
            );
        }
    }

    //현재 시간에서 최소 1일 이후의 납품 일시를 선택해야 한다.
    private static void validateRequestedDeliveryAt(LocalDateTime requestedDeliveryAt, LocalDateTime now){
        if(requestedDeliveryAt == null){
            throw new BusinessException(OrderErrorCode.REQUESTED_DELIVERY_AT_REQUIRED);
        }

        LocalDateTime minimumDeliveryAt = now.plusDays(1);

        if(requestedDeliveryAt.isBefore(minimumDeliveryAt)){
            throw new BusinessException(OrderErrorCode.INVALID_REQUESTED_DELIVERY_AT);
        }
    }

    private void validateCancelable(){
        if (status == OrderStatus.CANCELED) {
            throw new BusinessException(
                    OrderErrorCode.ORDER_ALREADY_CANCELED
            );
        }
        if (this.status != OrderStatus.PENDING) {
            throw new BusinessException(
                    OrderErrorCode.ORDER_NOT_CANCELABLE
            );
        }
    }

}