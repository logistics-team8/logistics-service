package com.logistics.orderservice.application.service.command;

import com.logistics.common.exception.BusinessException;
import com.logistics.orderservice.application.exception.StockDecreaseException;
import com.logistics.orderservice.application.exception.StockDecreaseUnknownException;
import com.logistics.orderservice.application.exception.StockRestoreException;
import com.logistics.orderservice.application.port.ProductPort;
import com.logistics.orderservice.domain.model.OrderFailureReason;
import com.logistics.orderservice.error.OrderErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class StockProcessServiceTest {
    @Mock OrderStateService orderStateService;
    @Mock ProductPort productPort;
    @InjectMocks StockProcessService service;

    private UUID orderId;
    private List<ProductPort.StockItem> items;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        items = List.of(new ProductPort.StockItem(UUID.randomUUID(), 2));
    }

    @Test
    @DisplayName("재고 차감 성공 시 주문 상태를 변경하지 않는다")
    void decrease_success() {
        assertThatCode(() -> service.decreaseStock(orderId, items)).doesNotThrowAnyException();
        verify(productPort).decreaseStock(items);
        verifyNoInteractions(orderStateService);
    }

    @Test
    @DisplayName("재고 차감 결과 미확인 시 주문에 미확인 상태를 기록한다")
    void decrease_unknown() {
        willThrow(new StockDecreaseUnknownException("timeout")).given(productPort).decreaseStock(items);
        assertBusinessException(() -> service.decreaseStock(orderId, items), OrderErrorCode.STOCK_DECREASE_UNKNOWN);
        verify(orderStateService).markStockDecreaseUnknown(orderId);
    }

    @Test
    @DisplayName("재고 차감 실패 시 주문을 실패 처리한다")
    void decrease_failed() {
        willThrow(new StockDecreaseException("failed")).given(productPort).decreaseStock(items);
        assertBusinessException(() -> service.decreaseStock(orderId, items), OrderErrorCode.STOCK_DECREASE_FAILED);
        verify(orderStateService).failOrder(orderId, OrderFailureReason.STOCK_DECREASE_FAILED);
    }

    @Test
    @DisplayName("배송 실패 후 재고 복원 실패 시 주문 실패 사유와 예외를 남긴다")
    void restoreAfterDeliveryFailure_failed() {
        willThrow(new StockRestoreException("failed")).given(productPort).restoreStock(items);
        assertBusinessException(() -> service.restoreStockAfterDeliveryFailure(orderId, items),
                OrderErrorCode.STOCK_RESTORE_FAILED);
        verify(orderStateService).failOrder(orderId, OrderFailureReason.STOCK_RESTORE_FAILED);
    }

    private void assertBusinessException(Runnable action, OrderErrorCode code) {
        assertThatThrownBy(action::run).isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(code));
    }
}
