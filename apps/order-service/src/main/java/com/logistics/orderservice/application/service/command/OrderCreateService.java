package com.logistics.orderservice.application.service.command;

import com.logistics.common.exception.BusinessException;
import com.logistics.common.security.principal.CustomUserDetails;
import com.logistics.orderservice.application.command.CreateOrderCommand;
import com.logistics.orderservice.application.command.CreateOrderItemCommand;
import com.logistics.orderservice.application.exception.*;
import com.logistics.orderservice.application.port.CompanyPort;
import com.logistics.orderservice.application.port.DeliveryPort;
import com.logistics.orderservice.application.port.ProductPort;
import com.logistics.orderservice.application.port.UserPort;
import com.logistics.orderservice.domain.model.Order;
import com.logistics.orderservice.domain.model.OrderFailureReason;
import com.logistics.orderservice.error.OrderErrorCode;
import com.logistics.orderservice.presentation.dto.response.CreateOrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCreateService {
    private final UserPort userPort;
    private final CompanyPort companyPort;
    private final ProductPort productPort;
    private final Clock clock;
    private final OrderStateService orderStateService;
    private final DeliveryRequestService deliveryRequestService;
    private final StockProcessService stockProcessService;


    /**
     * 주문 생성 전체 프로세스
     *
     * 상품 조회
     *   ↓
     * 주문 PENDING 저장
     *   ↓
     * 재고 차감
     *   ↓
     * CONFIRMED
     *   ↓
     * 배송 생성
     *   ↓
     * DELIVERY_CREATED
     */

    public CreateOrderResponse createOrder(CreateOrderCommand command, CustomUserDetails user) {
        LocalDateTime now = LocalDateTime.now(clock);

        //중복 상품 검증
        validateDuplicateProducts(command.items());

        //주문 요청에 들어있는 productId를 추출
        List<UUID> requestedProductIds = command.items()
                .stream()
                .map(CreateOrderItemCommand::productId)
                .toList();

        //상품 정보 다건 조회
        List<ProductPort.ProductInfo> products = productPort.getProducts(requestedProductIds);

        //요청한 상품들이 실제 존재하는 지 검증
        validateAllProductsExist(requestedProductIds, products);

        //하나의 주문은 하나의 배송을 만들기 위해 모든 상품의 출발 hub가 동일한지 확인
        UUID departureHubId = resolveDepartureHub(products);

        //응답 순서를 보장할 수 없으므로 MAP으로 변환
        Map<UUID, ProductPort.ProductInfo> productMap =
                products.stream()
                        .collect(Collectors.toMap(
                                ProductPort.ProductInfo::id,
                                Function.identity()
                        ));

        //주문 수령인의 정보 조회
        UserPort.UserInfo receiver = userPort.getUserInfo(user.getId());
        //수령 업체 정보 조회
        CompanyPort.CompanyInfo receiverCompany = companyPort.getCompanyInfo(command.receiverCompanyId());


        //주문 생성
        Order order = Order.create(
                generateOrderNumber(now),
                user.getId(),
                receiverCompany.id(),
                receiverCompany.hubId(),
                receiverCompany.address(),
                receiver.name(),
                receiver.slackId(),
                command.requestMessage(),
                command.requestedDeliveryAt(),
                now
        );

        //조회한 상품 정보 주문상품을 생성
        for (CreateOrderItemCommand item : command.items()) {
            ProductPort.ProductInfo product = productMap.get(item.productId());
            order.addOrderItem(
                    product.id(),
                    product.name(),
                    product.companyId(),
                    product.hubId(),
                    item.quantity()
            );
        }

        //PENDING 상태의 주문을 먼저 저장
        Order pendingOrder = orderStateService.createPendingOrder(order);

        UUID orderId = pendingOrder.getId();

        // 재고 처리할 상품의 요청 데이터 생성
        List<ProductPort.StockItem> stockItems = order.getOrderItems()
                .stream()
                .map(item ->
                        new ProductPort.StockItem(
                                item.getProductId(),
                                item.getQuantity()
                        )
                )
                .toList();


        //재고 차감
        stockProcessService.decreaseStock(orderId, stockItems);
        //재고 차감 성공 시 PENDING -> CONFIRMED
        orderStateService.confirmOrder(orderId);

        //배송 요청 정보 생성
        DeliveryPort.CreateDeliveryCommand deliveryCommand =
                new DeliveryPort.CreateDeliveryCommand(
                        orderId,
                        user.getId(),
                        departureHubId,
                        receiverCompany.hubId(),
                        receiverCompany.address(),
                        receiver.name(),
                        receiver.slackId()
                );


        Optional<DeliveryPort.DeliveryInfo> delivery;


        //배송 생성 요청
        try{
           delivery = deliveryRequestService.requestDelivery(deliveryCommand);
        }catch (DeliveryStatusUnknownException e){
            log.error("배송 생성 여부 확인 불가. orderId : {}", orderId, e);
            //배송이 실제 생성 될 수 있으므로 FAILED로 처리하지 않고, 재고도 복원하지 않는다.
            //CONFIRMED 상태를 유지,failureReason만 기록
            orderStateService.markDeliveryStatusCheckFailed(orderId);
            throw new BusinessException(OrderErrorCode.DELIVERY_STATUS_CHECK_FAILED);
        }


        // 배송을 조회 했는데 배송이 없다면 CONFIRMED 상태에서 재고 복원 후 FAILED 상태로 변환시킨다.
        if(delivery.isEmpty()){
            stockProcessService.restoreStockAfterDeliveryFailure(orderId, stockItems);

            //재고 복원 성공 시 CONFIRMED -> FAILED
            orderStateService.failOrder(orderId, OrderFailureReason.DELIVERY_CREATE_FAILED);

            throw new BusinessException(OrderErrorCode.DELIVERY_CREATE_FAILED);
        }


        //배송이 확인 되어 주문->배송 성공 CONFIRMED에서 DELIVERY_CREATED 상태로 변환한다.
        Order successOrder = orderStateService.markDeliveryCreated(orderId);
        //주문 생성 완료
        return CreateOrderResponse.from(successOrder);
    }


    /**
     * 주문 번호 생성 메서드
     * //ex)ORD-20260804-A12F45C98D01
     */
    private String generateOrderNumber(LocalDateTime now) {
        String date = now.toLocalDate()
                .format(DateTimeFormatter.BASIC_ISO_DATE);


        String suffix = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12)
                .toUpperCase();

        return "ORD-" + date + "-" + suffix;
    }


    private void validateAllProductsExist( List<UUID> requestedProductIds, List<ProductPort.ProductInfo> products) {
        if(products == null){
            throw new BusinessException(OrderErrorCode.PRODUCT_NOT_FOUND);
        }

        Set<UUID> foundProductIds = products
                .stream()
                .map(ProductPort.ProductInfo::id)
                .collect(Collectors.toSet());

        boolean missingProduct = requestedProductIds.stream()
                .anyMatch(productId -> !foundProductIds.contains(productId));

        if(missingProduct){
            throw new BusinessException(OrderErrorCode.PRODUCT_NOT_FOUND);
        }
    }


    private UUID resolveDepartureHub(List<ProductPort.ProductInfo> products){
        UUID departureHubId = products.getFirst().hubId();;

        boolean hasDifferentHub = products.stream()
                .anyMatch(product ->
                        product.hubId() == null || !departureHubId.equals(product.hubId()
                        )
                );

        if(hasDifferentHub){
            throw new BusinessException(OrderErrorCode.DIFFERENT_DEPARTURE_HUB);
        }

        return departureHubId;
    }

    //중복 상품 검증 메서드
    private void validateDuplicateProducts(List<CreateOrderItemCommand> items) {
        Set<UUID> productIds = new HashSet<>();
        for (CreateOrderItemCommand item : items) {
            if(!productIds.add(item.productId())){
                throw new BusinessException(OrderErrorCode.DUPLICATE_ORDER_PRODUCT);
            }
        }
    }



}
