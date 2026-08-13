package com.logistics.orderservice.infrastructure.client.product;

import com.logistics.common.exception.BusinessException;
import com.logistics.orderservice.application.exception.*;
import com.logistics.orderservice.application.port.ProductPort;
import com.logistics.orderservice.error.OrderErrorCode;
import feign.FeignException;
import feign.RetryableException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProductClientAdapter implements ProductPort {

    private final ProductFeignClient  productFeignClient;

    @Override
    public List<ProductInfo> getProducts(List<UUID> productIds) {
        try {
            List<ProductFeignClient.ProductResponse> responses = productFeignClient.getProducts(productIds).getData();

            if (responses == null) {
                throw new BusinessException(OrderErrorCode.PRODUCT_NOT_FOUND);
            }

            return responses.stream()
                    .map(response -> new ProductInfo(
                            response.id(),
                            response.name(),
                            response.companyId(),
                            response.hubId()
                    ))
                    .toList();

        } catch (FeignException.NotFound exception) {
            throw new BusinessException(
                    OrderErrorCode.PRODUCT_NOT_FOUND
            );
        }
    }

    @Override
    public void decreaseStock(UUID orderId, List<StockItem> items) {
        try {
            productFeignClient.decreaseStock(toRequest(orderId, items));

        } catch (RetryableException e){
            throw new StockDecreaseUnknownException( "재고 차감 처리 결과를 확인할 수 없습니다.", e);
        }

        catch (FeignException e) {
            if (isTransientStatus(e.status())) {
                throw new StockDecreaseUnknownException(
                        "재고 서비스의 일시적 장애로 차감 결과를 확인할 수 없습니다.", e
                );
            }
            throw new StockDecreaseException("재고 차감 요청이 거절되었습니다.", e);
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
            if (isTransientStatus(e.status())) {
                throw new StockRestoreUnknownException(
                        "재고 서비스의 일시적 장애로 복원 결과를 확인할 수 없습니다.", e
                );
            }
            throw new StockRestoreException("재고 복원 요청이 거절되었습니다.", e);
        }
    }

    @Override
    public boolean isStockDecreased(UUID orderId) {
        try {
            Boolean result = productFeignClient.isStockDecreased(orderId).getData();

            //NULL -> 처리 상태를 알 수 없는 잘못된 응답
            //NULL을 FALSE로 취급하지 말자
            if(result == null) {
                throw new StockStatusLookupException("재고 차감 처리 상태 응답이 없습니다.");
            }
            return result;
        }
        catch (FeignException e) {
            throw new StockStatusLookupException("재고 차감 상태 조회에 실패했습니다.", e);
        }
    }

    @Override
    public boolean isStockRestored(UUID orderId) {
        try{
            Boolean result = productFeignClient.isStockRestored(orderId).getData();

            if(result == null) {
                throw new StockStatusLookupException("재고 복원 처리 상태 응답이 없습니다.");
            }

            return result;
        }
        catch (FeignException e) {
            throw new StockStatusLookupException(  "재고 복원 상태 조회에 실패했습니다.", e);
        }
    }

    private boolean isTransientStatus(int status) {
        return status == 502 || status == 503 || status == 504;
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
