package com.logistics.orderservice.application.service.command;

import com.logistics.common.exception.BusinessException;
import com.logistics.orderservice.application.exception.StockDecreaseException;
import com.logistics.orderservice.application.exception.StockDecreaseUnknownException;
import com.logistics.orderservice.application.exception.StockRestoreException;
import com.logistics.orderservice.application.exception.StockRestoreUnknownException;
import com.logistics.orderservice.application.exception.StockStatusLookupException;
import com.logistics.orderservice.application.port.ProductPort;
import com.logistics.orderservice.domain.model.OrderFailureReason;
import com.logistics.orderservice.error.OrderErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class StockProcessServiceTest {

    @Mock private OrderStateService orderStateService;
    @Mock private ProductPort productPort;
    @InjectMocks private StockProcessService service;

    private UUID orderId;
    private List<ProductPort.StockItem> items;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        items = List.of(new ProductPort.StockItem(UUID.randomUUID(), 2));
    }

    @Nested
    @DisplayName("재고 차감")
    class DecreaseStock {

        @Test
        @DisplayName("성공하면 동일 orderId와 상품 목록을 전달한다")
        void success() {
            assertThatCode(() -> service.decreaseStock(orderId, items))
                    .doesNotThrowAnyException();

            verify(productPort).decreaseStock(orderId, items);
            verifyNoInteractions(orderStateService);
        }

        @Test
        @DisplayName("응답 불명이어도 처리 완료가 조회되면 재시도하지 않는다")
        void unknown_butAlreadyProcessed() {
            willThrow(new StockDecreaseUnknownException("timeout"))
                    .given(productPort).decreaseStock(orderId, items);
            given(productPort.isStockDecreased(orderId)).willReturn(true);

            assertThatCode(() -> service.decreaseStock(orderId, items))
                    .doesNotThrowAnyException();

            verify(productPort).isStockDecreased(orderId);
            verify(productPort).decreaseStock(orderId, items);
            verifyNoInteractions(orderStateService);
        }

        @Test
        @DisplayName("응답 불명이고 미처리 상태면 동일 orderId로 한 번 재시도한다")
        void unknown_notProcessed_retriesOnce() {
            willThrow(new StockDecreaseUnknownException("timeout"))
                    .willDoNothing()
                    .given(productPort).decreaseStock(orderId, items);
            given(productPort.isStockDecreased(orderId)).willReturn(false);

            assertThatCode(() -> service.decreaseStock(orderId, items))
                    .doesNotThrowAnyException();

            verify(productPort).isStockDecreased(orderId);
            verify(productPort, times(2)).decreaseStock(orderId, items);
            verifyNoInteractions(orderStateService);
        }

        @Test
        @DisplayName("응답 불명 후 상태 조회도 실패하면 PENDING 주문에 불명 상태를 기록한다")
        void unknown_lookupFailure() {
            willThrow(new StockDecreaseUnknownException("timeout"))
                    .given(productPort).decreaseStock(orderId, items);
            willThrow(new StockStatusLookupException("lookup failed"))
                    .given(productPort).isStockDecreased(orderId);

            assertBusinessException(
                    () -> service.decreaseStock(orderId, items),
                    OrderErrorCode.STOCK_DECREASE_UNKNOWN
            );

            verify(orderStateService).markStockDecreaseUnknown(orderId);
        }

        @Test
        @DisplayName("명확한 차감 실패면 주문을 실패 처리한다")
        void failed() {
            willThrow(new StockDecreaseException("failed"))
                    .given(productPort).decreaseStock(orderId, items);

            assertBusinessException(
                    () -> service.decreaseStock(orderId, items),
                    OrderErrorCode.STOCK_DECREASE_FAILED
            );

            verify(orderStateService).failOrder(
                    orderId,
                    OrderFailureReason.STOCK_DECREASE_FAILED
            );
        }
    }

    @Nested
    @DisplayName("배송 실패 후 재고 복원")
    class RestoreAfterDeliveryFailure {

        @Test
        @DisplayName("응답 불명이어도 복원 완료가 조회되면 재시도하지 않는다")
        void unknown_butAlreadyRestored() {
            willThrow(new StockRestoreUnknownException("timeout"))
                    .given(productPort).restoreStock(orderId, items);
            given(productPort.isStockRestored(orderId)).willReturn(true);

            assertThatCode(() -> service.restoreStockAfterDeliveryFailure(orderId, items))
                    .doesNotThrowAnyException();

            verify(productPort).isStockRestored(orderId);
            verify(productPort).restoreStock(orderId, items);
            verifyNoInteractions(orderStateService);
        }

        @Test
        @DisplayName("응답 불명이고 미복원 상태면 동일 orderId로 한 번 재시도한다")
        void unknown_notRestored_retriesOnce() {
            willThrow(new StockRestoreUnknownException("timeout"))
                    .willDoNothing()
                    .given(productPort).restoreStock(orderId, items);
            given(productPort.isStockRestored(orderId)).willReturn(false);

            assertThatCode(() -> service.restoreStockAfterDeliveryFailure(orderId, items))
                    .doesNotThrowAnyException();

            verify(productPort, times(2)).restoreStock(orderId, items);
            verifyNoInteractions(orderStateService);
        }

        @Test
        @DisplayName("복원 상태 조회 실패면 주문 실패 원인을 UNKNOWN으로 기록한다")
        void unknown_lookupFailure() {
            willThrow(new StockRestoreUnknownException("timeout"))
                    .given(productPort).restoreStock(orderId, items);
            willThrow(new StockStatusLookupException("lookup failed"))
                    .given(productPort).isStockRestored(orderId);

            assertBusinessException(
                    () -> service.restoreStockAfterDeliveryFailure(orderId, items),
                    OrderErrorCode.STOCK_RESTORE_UNKNOWN
            );

            verify(orderStateService).failOrder(
                    orderId,
                    OrderFailureReason.STOCK_RESTORE_UNKNOWN
            );
        }

        @Test
        @DisplayName("명확한 복원 실패면 주문 실패 사유를 기록한다")
        void failed() {
            willThrow(new StockRestoreException("failed"))
                    .given(productPort).restoreStock(orderId, items);

            assertBusinessException(
                    () -> service.restoreStockAfterDeliveryFailure(orderId, items),
                    OrderErrorCode.STOCK_RESTORE_FAILED
            );

            verify(orderStateService).failOrder(
                    orderId,
                    OrderFailureReason.STOCK_RESTORE_FAILED
            );
        }
    }

    @Nested
    @DisplayName("주문 취소 재고 복원")
    class RestoreForCancel {

        @Test
        @DisplayName("복원 상태 조회 실패면 주문 상태를 변경하지 않는다")
        void unknown_lookupFailure() {
            willThrow(new StockRestoreUnknownException("timeout"))
                    .given(productPort).restoreStock(orderId, items);
            willThrow(new StockStatusLookupException("lookup failed"))
                    .given(productPort).isStockRestored(orderId);

            assertBusinessException(
                    () -> service.restoreStockForCancel(orderId, items),
                    OrderErrorCode.ORDER_CANCEL_STOCK_RESTORE_UNKNOWN
            );

            verifyNoInteractions(orderStateService);
        }

        @Test
        @DisplayName("명확한 복원 실패면 취소 실패 예외를 발생시키고 주문 상태를 유지한다")
        void failed() {
            willThrow(new StockRestoreException("failed"))
                    .given(productPort).restoreStock(orderId, items);

            assertBusinessException(
                    () -> service.restoreStockForCancel(orderId, items),
                    OrderErrorCode.ORDER_CANCEL_STOCK_RESTORE_FAILED
            );

            verify(orderStateService, never()).cancelOrder(
                    org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any()
            );
        }
    }

    private void assertBusinessException(Runnable action, OrderErrorCode code) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(
                        ((BusinessException) exception).getErrorCode()
                ).isEqualTo(code));
    }
}
