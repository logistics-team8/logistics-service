package com.logistics.orderservice.application.port;

import java.util.List;
import java.util.UUID;

public interface ProductPort {
    List<ProductInfo> getProducts(List<UUID> productIds);

    void decreaseStock(List<StockItem> items);

    void restoreStock(List<StockItem> items);


    record ProductInfo(
            UUID id,
            String name,
            UUID companyId,
            UUID hubId
    ){}


    record StockItem(
            UUID productId,
            Integer quantity
    ){}

}
