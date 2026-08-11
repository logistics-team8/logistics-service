package com.logistics.orderservice.infrastructure.client.product;

import com.logistics.orderservice.application.port.ProductPort;
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
    public List<ProductInfo> getProducts(List<UUID> productIds){
        return productFeignClient.getProducts(productIds)
                .stream()
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
