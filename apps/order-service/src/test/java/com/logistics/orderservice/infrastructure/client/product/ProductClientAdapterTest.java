package com.logistics.orderservice.infrastructure.client.product;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

import com.logistics.orderservice.application.exception.StockDecreaseException;
import com.logistics.orderservice.application.exception.StockDecreaseUnknownException;
import com.logistics.orderservice.application.exception.StockRestoreException;
import com.logistics.orderservice.application.exception.StockRestoreUnknownException;
import com.logistics.orderservice.application.port.ProductPort;
import feign.FeignException;
import feign.Request;
import feign.Response;
import feign.RetryableException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductClientAdapterTest {

    @Mock ProductFeignClient productFeignClient;

    private ProductClientAdapter adapter;
    private UUID orderId;
    private List<ProductPort.StockItem> items;

    @BeforeEach
    void setUp() {
        adapter = new ProductClientAdapter(productFeignClient);
        orderId = UUID.randomUUID();
        items = List.of(new ProductPort.StockItem(UUID.randomUUID(), 1));
    }

    @Test
    @DisplayName("재고 차감 연결 실패는 처리 결과 불명으로 분류한다")
    void decreaseStock_retryableExceptionIsUnknown() {
        willThrow(retryableException(Request.HttpMethod.PATCH))
                .given(productFeignClient).decreaseStock(any());

        assertThatThrownBy(() -> adapter.decreaseStock(orderId, items))
                .isInstanceOf(StockDecreaseUnknownException.class);
    }

    @Test
    @DisplayName("재고 차감 502, 503, 504는 처리 결과 불명으로 분류한다")
    void decreaseStock_transientStatusIsUnknown() {
        for (int status : List.of(502, 503, 504)) {
            willThrow(feignException(status, Request.HttpMethod.PATCH))
                    .given(productFeignClient).decreaseStock(any());

            assertThatThrownBy(() -> adapter.decreaseStock(orderId, items))
                    .isInstanceOf(StockDecreaseUnknownException.class);
        }
    }

    @Test
    @DisplayName("재고 차감 400은 명확한 실패로 분류한다")
    void decreaseStock_badRequestIsRejected() {
        willThrow(feignException(400, Request.HttpMethod.PATCH))
                .given(productFeignClient).decreaseStock(any());

        assertThatThrownBy(() -> adapter.decreaseStock(orderId, items))
                .isInstanceOf(StockDecreaseException.class)
                .isNotInstanceOf(StockDecreaseUnknownException.class);
    }

    @Test
    @DisplayName("재고 복원 연결 실패와 503은 처리 결과 불명으로 분류한다")
    void restoreStock_transientFailureIsUnknown() {
        willThrow(retryableException(Request.HttpMethod.PATCH))
                .given(productFeignClient).restoreStock(any());
        assertThatThrownBy(() -> adapter.restoreStock(orderId, items))
                .isInstanceOf(StockRestoreUnknownException.class);

        willThrow(feignException(503, Request.HttpMethod.PATCH))
                .given(productFeignClient).restoreStock(any());
        assertThatThrownBy(() -> adapter.restoreStock(orderId, items))
                .isInstanceOf(StockRestoreUnknownException.class);
    }

    @Test
    @DisplayName("재고 복원 409는 명확한 실패로 분류한다")
    void restoreStock_conflictIsRejected() {
        willThrow(feignException(409, Request.HttpMethod.PATCH))
                .given(productFeignClient).restoreStock(any());

        assertThatThrownBy(() -> adapter.restoreStock(orderId, items))
                .isInstanceOf(StockRestoreException.class)
                .isNotInstanceOf(StockRestoreUnknownException.class);
    }

    private RetryableException retryableException(Request.HttpMethod method) {
        Request request = request(method);
        return new RetryableException(
                -1,
                "connection failed",
                method,
                new java.io.IOException("connection failed"),
                (Long) null,
                request
        );
    }

    private FeignException feignException(int status, Request.HttpMethod method) {
        Request request = request(method);
        Response response = Response.builder()
                .status(status)
                .reason("test")
                .request(request)
                .headers(Map.of())
                .build();
        return FeignException.errorStatus("ProductFeignClient", response);
    }

    private Request request(Request.HttpMethod method) {
        return Request.create(
                method,
                "http://company-product-service/internal/v1/products/stock",
                Map.of(),
                new byte[0],
                StandardCharsets.UTF_8
        );
    }
}
