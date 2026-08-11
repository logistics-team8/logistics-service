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

    //재고 차감 메서드
    public void decreaseStock( UUID orderId, List<ProductPort.StockItem> stockItems){
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
    }


    //재고 보상 메서드
    public void restoreStockAfterDeliveryFailure(UUID orderId, List<ProductPort.StockItem> stockItems) {
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


    //주문 취소 재고 복원
    public void restoreStockForCancel(UUID orderId, List<ProductPort.StockItem> restoreItems) {
        try{
            productPort.restoreStock(restoreItems);
        }catch (StockRestoreUnknownException e) {
            log.error("주문 취소 중 재고 복원 결과 확인 불가. orderId={}", orderId, e);
            throw new BusinessException(OrderErrorCode.ORDER_CANCEL_STOCK_RESTORE_UNKNOWN);
        }

        catch (StockRestoreException e){
            log.error("주문 취소 중 재고 복원 실패. orderId : {}", orderId, e);
            throw new BusinessException(OrderErrorCode.ORDER_CANCEL_STOCK_RESTORE_FAILED);
        }

    }
}
