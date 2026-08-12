package com.logistics.orderservice.infrastructure.client.product;

import com.logistics.common.exception.BusinessException;
import com.logistics.orderservice.application.exception.StockDecreaseException;
import com.logistics.orderservice.application.exception.StockDecreaseUnknownException;
import com.logistics.orderservice.application.exception.StockRestoreException;
import com.logistics.orderservice.application.exception.StockRestoreUnknownException;
import com.logistics.orderservice.application.port.ProductPort;
import com.logistics.orderservice.error.OrderErrorCode;
import feign.FeignException;
import feign.RetryableException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProductClientAdapter implements ProductPort {

    private final ProductFeignClient  productFeignClient;

    @Override
    public List<ProductInfo> getProducts(
            List<UUID> productIds
    ) {
        List<ProductFeignClient.ProductResponse> responses = productFeignClient
                        .getProducts(productIds)
                        .getData();

        if (responses == null) {
            throw new BusinessException(
                    OrderErrorCode.PRODUCT_NOT_FOUND
            );
        }

        return responses.stream()
                .map(response -> new ProductInfo(
                        response.id(),
                        response.name(),
                        response.companyId(),
                        response.hubId()
                ))
                .toList();
    }

    @Override
    public void decreaseStock(UUID orderId, List<StockItem> items) {
        try {
            productFeignClient.decreaseStock(toRequest(orderId, items));

        } catch (RetryableException e){
            throw new StockDecreaseUnknownException( "재고 차감 처리 결과를 확인할 수 없습니다.", e);
        }

        catch (FeignException e) {
            throw new StockDecreaseException("재고 차감 요청에 실패했습니다.", e);
        }
    }

    @Override
    public void restoreStock(UUID orderId, List<StockItem> items) {
        try {
            productFeignClient.restoreStock(toRequest(orderId, items));

        } catch (RetryableException e){
            throw new StockRestoreUnknownException("재고 복원 처리 결과를 확인할 수 없습니다.", e);
        }

        catch (FeignException e) {
            throw new StockRestoreException("재고 복원 요청에 실패했습니다.", e);
        }
    }

    @Override
    public boolean isStockDecreased(UUID orderId) {
        try {
            Boolean result = productFeignClient.isStockDecreased(orderId).getData();

            return Boolean.TRUE.equals(result);
        }catch (FeignException e) {
            throw new StockDecreaseUnknownException("재고 차감 상태 조회에 실패했습니다.", e);
        }
    }

    @Override
    public boolean isStockRestored(UUID orderId) {
        try{
            Boolean result = productFeignClient.isStockRestored(orderId).getData();
            return Boolean.TRUE.equals(result);
        }catch (FeignException e) {
            throw new StockRestoreUnknownException(  "재고 복원 상태 조회에 실패했습니다.", e);
        }
    }


    private ProductFeignClient.StockItemListRequest toRequest(UUID orderId, List<StockItem> items) {
        List<ProductFeignClient.StockItemRequest> requests =
                items.stream()
                        .map(item ->
                                new ProductFeignClient.StockItemRequest(
                                        item.productId(),
                                        item.quantity()
                                )

                        ).toList();
        return new ProductFeignClient.StockItemListRequest(orderId, requests);
    }
}
