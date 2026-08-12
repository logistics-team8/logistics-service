package com.logistics.orderservice.application.service.command;

import com.logistics.common.exception.BusinessException;
import com.logistics.orderservice.application.exception.StockDecreaseException;
import com.logistics.orderservice.application.exception.StockDecreaseUnknownException;
import com.logistics.orderservice.application.exception.StockRestoreException;
import com.logistics.orderservice.application.exception.StockRestoreUnknownException;
import com.logistics.orderservice.application.port.ProductPort;
import com.logistics.orderservice.domain.model.OrderFailureReason;
import com.logistics.orderservice.error.OrderErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockProcessService {

    private final OrderStateService orderStateService;
    private final ProductPort productPort;


    /**
     * 주문 생성 시 재고 차감
     *
     * 1. 재고 차감 요청
     * 2. 응답 결과를 알 수 없으면 Product 처리 상태 조회
     * 3. 처리되지 않았다면 동일 orderId로 1회 재요청
     */
    public void decreaseStock(UUID orderId, List<ProductPort.StockItem> stockItems) {
        try {
            productPort.decreaseStock(orderId, stockItems);
        } catch (StockDecreaseUnknownException e) {
            resolveDecreaseUnknown(orderId, stockItems);
        } catch (StockDecreaseException e) {
            failDecrease(orderId, e);
        }
    }


    /**
     * 배송 생성 실패 후 재고 복원
     *
     * 1. 재고 복원 요청
     * 2. 결과를 알 수 없으면 Product 처리 상태 조회
     * 3. 처리되지 않았다면 동일 orderId로 1회 재요청
     */
    public void restoreStockAfterDeliveryFailure(UUID orderId, List<ProductPort.StockItem> stockItems) {

        try {productPort.restoreStock(orderId, stockItems);
        } catch (StockRestoreUnknownException e) {
            resolveRestoreUnknownAfterDeliveryFailure(orderId, stockItems);
        } catch (StockRestoreException e) {
            failRestoreAfterDeliveryFailure(orderId, e);
        }
    }


    /**
     * 주문 취소 시 재고 복원
     *
     * 복원이 확인된 경우에만 호출한 OrderCancelService에서
     * 주문을 CANCELED로 변경한다.
     */
    public void restoreStockForCancel(UUID orderId, List<ProductPort.StockItem> stockItems) {
        try {
            productPort.restoreStock(orderId, stockItems);
        } catch (StockRestoreUnknownException e) {
            resolveRestoreUnknownForCancel(orderId, stockItems);
        } catch (StockRestoreException e) {
            failRestoreForCancel(orderId, e);
        }
    }


    /*
     * 재고 차감 ------------------------------------
     */


    /**
     * 재고 차감 요청의 결과를 알 수 없는 경우.
     *
     * Product에 해당 orderId의 재고 차감 처리 여부를 조회한다.
     */
    private void resolveDecreaseUnknown(UUID orderId, List<ProductPort.StockItem> stockItems
    ) {
        log.warn("재고 차감 응답 확인 불가. 처리 상태 조회. orderId : {}", orderId);

        boolean decreased = checkDecreaseStatus(orderId);
        /*
         * Product에서 차감 처리가 확인됐다면
         * 최초 요청은 성공한 것으로 판단한다.
         */
        if (decreased) {
            log.info("재고 차감 처리 확인. orderId : {}", orderId);return;
        }


        /*
         * 처리되지 않은 것으로 확인됐다면
         * 동일 orderId로 한 번 재시도한다.
         */
        retryDecrease(orderId, stockItems);
    }


    /**
     * Product에 재고 차감 처리 여부 조회.
     */
    private boolean checkDecreaseStatus(UUID orderId) {

        try {
            return productPort.isStockDecreased(orderId);

        } catch (StockDecreaseUnknownException e) {

            /*
             * 최초 차감 요청 결과도 모르고
             * 상태 조회까지 실패.
             *
             * 실제 차감 여부를 확정할 수 없다.
             */
            log.error("재고 차감 처리 상태 조회 실패. orderId : {}", orderId, e);

            orderStateService.markStockDecreaseUnknown(orderId);

            throw new BusinessException(OrderErrorCode.STOCK_DECREASE_UNKNOWN);
        }
    }


    /**
     * 재고 차감 1회 재시도.
     *
     * Product에서 orderId 기반 멱등성을 처리하므로
     * 동일 orderId를 그대로 전달한다.
     */
    private void retryDecrease(UUID orderId, List<ProductPort.StockItem> stockItems) {

        log.info("재고 차감 미처리 확인. 재요청. orderId : {}", orderId);


        try {

            productPort.decreaseStock(orderId, stockItems);

        } catch (StockDecreaseUnknownException e) {

            /*
             * 재시도 결과까지 알 수 없다면
             * 더 이상 자동 재시도하지 않는다.
             */
            log.error("재고 차감 재요청 결과 확인 불가. orderId : {}", orderId, e);

            orderStateService.markStockDecreaseUnknown(orderId);

            throw new BusinessException(OrderErrorCode.STOCK_DECREASE_UNKNOWN);

        } catch (StockDecreaseException e) {

            failDecrease(orderId, e);
        }
    }


    /**
     * 재고 차감 명확한 실패 처리.
     */
    private void failDecrease(UUID orderId, StockDecreaseException e) {

        log.error("재고 차감 실패. orderId : {}", orderId, e);


        orderStateService.failOrder(orderId, OrderFailureReason.STOCK_DECREASE_FAILED);


        throw new BusinessException(OrderErrorCode.STOCK_DECREASE_FAILED);
    }


    /*
     * 배송 생성 실패 후 재고 복원 -----------------------------------------
     */


    /**
     * 배송 생성 실패 후 재고 복원 요청의 결과를
     * 알 수 없는 경우.
     */
    private void resolveRestoreUnknownAfterDeliveryFailure(UUID orderId, List<ProductPort.StockItem> stockItems) {

        log.warn("재고 복원 응답 확인 불가. 처리 상태 조회. orderId : {}", orderId);

        boolean restored = checkRestoreStatusAfterDeliveryFailure(orderId);


        /*
         * Product에서 복원 처리가 확인됐으면
         * 보상이 성공한 것으로 판단한다.
         */
        if (restored) {
            log.info("재고 복원 처리 확인. orderId : {}", orderId);
            return;
        }


        /*
         * 복원되지 않은 것으로 확인됐다면
         * 동일 orderId로 한 번 재요청.
         */
        retryRestoreAfterDeliveryFailure(orderId, stockItems);
    }


    /**
     * 배송 실패 보상 과정에서
     * Product 재고 복원 처리 여부 조회.
     */
    private boolean checkRestoreStatusAfterDeliveryFailure(UUID orderId) {

        try {
            return productPort.isStockRestored(orderId);
        } catch (StockRestoreUnknownException e) {

            /*
             * 복원 요청 결과도 모르고
             * 조회까지 실패했으므로
             * 실제 재고 상태를 확정할 수 없다.
             */
            log.error("재고 복원 처리 상태 조회 실패. orderId : {}", orderId, e);
            orderStateService.failOrder(orderId, OrderFailureReason.STOCK_RESTORE_UNKNOWN);
            throw new BusinessException(OrderErrorCode.STOCK_RESTORE_UNKNOWN);
        }
    }


    /**
     * 배송 실패 후 재고 복원 1회 재시도.
     */
    private void retryRestoreAfterDeliveryFailure(UUID orderId, List<ProductPort.StockItem> stockItems) {

        log.info("재고 복원 미처리 확인. 재요청. orderId : {}", orderId);


        try {
            productPort.restoreStock(orderId, stockItems);

        } catch (StockRestoreUnknownException e) {

            log.error("재고 복원 재요청 결과 확인 불가. orderId : {}", orderId, e);
            orderStateService.failOrder(orderId, OrderFailureReason.STOCK_RESTORE_UNKNOWN);
            throw new BusinessException(OrderErrorCode.STOCK_RESTORE_UNKNOWN);

        } catch (StockRestoreException e) {
            failRestoreAfterDeliveryFailure(orderId, e);
        }
    }


    /**
     * 배송 생성 실패 후 재고 복원이
     * 명확하게 실패한 경우.
     */
    private void failRestoreAfterDeliveryFailure(UUID orderId, StockRestoreException e) {

        log.error("배송 생성 실패 후 재고 복원 실패. orderId : {}", orderId, e);
        orderStateService.failOrder(orderId, OrderFailureReason.STOCK_RESTORE_FAILED);
        throw new BusinessException(OrderErrorCode.STOCK_RESTORE_FAILED);
    }


    /*
     * 주문 취소 시 재고 복원 -------------------------------------------
     */


    /**
     * 주문 취소 중 재고 복원 요청의 결과를
     * 알 수 없는 경우.
     */
    private void resolveRestoreUnknownForCancel(UUID orderId, List<ProductPort.StockItem> stockItems) {

        log.warn("주문 취소 중 재고 복원 응답 확인 불가. " + "처리 상태 조회. orderId : {}", orderId);
        boolean restored = checkRestoreStatusForCancel(orderId);


        /*
         * 복원이 이미 확인되었다면
         * OrderCancelService가 CANCELED로 진행할 수 있다.
         */
        if (restored) {
            log.info("주문 취소 재고 복원 처리 확인. orderId : {}", orderId);
            return;
        }

        /*
         * 복원되지 않았다면
         * 동일 orderId로 한 번 재요청.
         */
        retryRestoreForCancel(orderId, stockItems);
    }


    /**
     * 주문 취소 재고 복원 처리 여부 조회.
     */
    private boolean checkRestoreStatusForCancel(UUID orderId) {

        try {
            return productPort.isStockRestored(orderId);

        } catch (StockRestoreUnknownException e) {

            /*
             * 재고가 실제로 복원됐는지 알 수 없으므로
             * 주문을 CANCELED로 변경하면 안 된다.
             */
            log.error("주문 취소 중 재고 복원 상태 조회 실패. orderId : {}", orderId, e);
            throw new BusinessException(OrderErrorCode.ORDER_CANCEL_STOCK_RESTORE_UNKNOWN);
        }
    }


    /**
     * 주문 취소 재고 복원 1회 재시도.
     */
    private void retryRestoreForCancel(
            UUID orderId,
            List<ProductPort.StockItem> stockItems
    ) {

        log.info("주문 취소 재고 복원 미처리 확인. 재요청. orderId : {}", orderId);


        try {
            productPort.restoreStock(orderId, stockItems);

        } catch (StockRestoreUnknownException e) {

            log.error("주문 취소 중 재고 복원 재요청 결과 확인 불가. orderId : {}", orderId, e);

            throw new BusinessException(OrderErrorCode.ORDER_CANCEL_STOCK_RESTORE_UNKNOWN);

        } catch (StockRestoreException e) {
            failRestoreForCancel(orderId, e);
        }
    }


    /**
     * 주문 취소 중 재고 복원이
     * 명확하게 실패한 경우.
     *
     * 여기서는 Order 상태를 FAILED로 만들지 않는다.
     * 재고 복원이 실패했기 때문에 취소 자체를 실패시키고
     * 기존 CONFIRMED 상태를 유지한다.
     */
    private void failRestoreForCancel(
            UUID orderId,
            StockRestoreException e
    ) {
        log.error("주문 취소 중 재고 복원 실패. orderId : {}", orderId, e);
        throw new BusinessException(OrderErrorCode.ORDER_CANCEL_STOCK_RESTORE_FAILED);
    }
}
