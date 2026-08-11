package com.logistics.orderservice.application.service.command;

import com.logistics.common.exception.BusinessException;
import com.logistics.common.security.principal.CustomUserDetails;
import com.logistics.orderservice.application.authorization.OrderAuthorization;
import com.logistics.orderservice.application.command.CreateOrderCommand;
import com.logistics.orderservice.application.command.CreateOrderItemCommand;
import com.logistics.orderservice.application.exception.*;
import com.logistics.orderservice.application.port.CompanyPort;
import com.logistics.orderservice.application.port.DeliveryPort;
import com.logistics.orderservice.application.port.ProductPort;
import com.logistics.orderservice.application.port.UserPort;
import com.logistics.orderservice.domain.model.Order;
import com.logistics.orderservice.domain.model.OrderFailureReason;
import com.logistics.orderservice.domain.repository.OrderRepository;
import com.logistics.orderservice.error.OrderErrorCode;
import com.logistics.orderservice.presentation.dto.response.CreateOrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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



    public CreateOrderResponse createOrder(CreateOrderCommand command, CustomUserDetails user) {
        LocalDateTime now = LocalDateTime.now(clock);

        //중복 상품 검증
        validateDuplicateProducts(command.items());


        //주문 상품 조회
        List<UUID> requestedProductIds = command.items()
                .stream()
                .map(CreateOrderItemCommand::productId)
                .toList();

        //상품 정보 다건 조회
        List<ProductPort.ProductInfo> products = productPort.getProducts(requestedProductIds);

        //요청한 상품들이 모두 조회됬는지 검증한다.
        validateAllProductsExist(requestedProductIds, products);

        //모든 상품의 출발 hub가 같은지 검사
        UUID departureHubId = resolveDepartureHub(products);

        //응답 순서를 보장할 수 없으므로 MAP으로 변환
        Map<UUID, ProductPort.ProductInfo> productMap =
                products.stream()
                        .collect(Collectors.toMap(
                                ProductPort.ProductInfo::id,
                                Function.identity()
                        ));

        //주문 수령인 조회
        UserPort.UserInfo receiver = userPort.getUserInfo(user.getId());
        //수령 업체 조회
        CompanyPort.CompanyInfo receiverCompany = companyPort.getCompanyInfo(command.receiverCompanyId());


        //주문 생성
        Order order = Order.create(
                generateOrderNumber(now),
                user.getId(),
                //command.receiverCompanyId(),
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

        //PENDING 상태의 주문 저장
        Order pendingOrder = orderStateService.createPendingOrder(order);

        UUID orderId = pendingOrder.getId();

        // 재고 차감할 상품의 요청 데이터 생성
        List<ProductPort.StockItem> stockItems = order.getOrderItems()
                .stream()
                .map(item ->
                        new ProductPort.StockItem(
                                item.getProductId(),
                                item.getQuantity()
                        )
                )
                .toList();


        //재고 차감 요청
        try {
            productPort.decreaseStock(stockItems);
        } catch (StockDecreaseUnknownException e){
            log.error("재고 차감 결과 확인 불가 orderId : {}", orderId, e);
            orderStateService.markStockDecreaseUnknown(orderId);
            throw new BusinessException(OrderErrorCode.STOCK_DECREASE_UNKNOWN);
        }

        catch (StockDecreaseException e) {
            log.error("재고 차감 실패 orderId : {}", orderId, e);

            orderStateService.failOrder(orderId, OrderFailureReason.STOCK_DECREASE_FAILED);
            throw new BusinessException(OrderErrorCode.STOCK_DECREASE_FAILED);
        }

        //재고 차감 성공
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

        //배송 생성 요청
        Optional<DeliveryPort.DeliveryInfo> delivery;


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
            restoreStockAfterDeliveryFailure(orderId, stockItems);

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


    //재고 보상 메서드
    private void restoreStockAfterDeliveryFailure(UUID orderId, List<ProductPort.StockItem> stockItems) {
        try{
            productPort.restoreStock(stockItems);
        }catch (StockRestoreUnknownException e){
            orderStateService.failOrder(orderId, OrderFailureReason.STOCK_RESTORE_UNKNOWN);
            log.error("배송 생성 실패 후 재고 복원 결과 확인 불가. orderId : {}", orderId, e);
        }
        catch (StockRestoreException e){
            log.error( "배송 생성 실패 후 재고 복원 실패. orderId={}", orderId, e);

            orderStateService.failOrder(orderId, OrderFailureReason.STOCK_RESTORE_FAILED);
            throw new BusinessException(OrderErrorCode.STOCK_RESTORE_FAILED);
        }
    }


}
