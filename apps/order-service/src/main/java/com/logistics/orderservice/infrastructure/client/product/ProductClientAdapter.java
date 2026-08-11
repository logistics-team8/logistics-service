package com.logistics.orderservice.infrastructure.client.product;

import com.logistics.common.exception.BusinessException;
import com.logistics.orderservice.application.port.ProductPort;
import com.logistics.orderservice.error.OrderErrorCode;
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
    public void decreaseStock(List<StockItem> items) {
        productFeignClient.decreaseStock(toRequest(items));
    }

    @Override
    public void restoreStock(List<StockItem> items) {
        productFeignClient.restoreStock(toRequest(items));
    }


    private ProductFeignClient.StockItemListRequest toRequest(List<StockItem> items) {
        List<ProductFeignClient.StockItemRequest> requests =
                items.stream()
                        .map(item ->
                                new ProductFeignClient.StockItemRequest(
                                        item.productId(),
                                        item.quantity()
                                )

                        ).toList();
        return new ProductFeignClient.StockItemListRequest(requests);
    }
}
