package com.logistics.orderservice.application.port;

import java.util.List;
import java.util.UUID;

public interface ProductPort {
    List<ProductInfo> getProducts(List<UUID> productIds);

    void decreaseStock(UUID orderId, List<StockItem> items);
    void restoreStock(UUID orderId, List<StockItem> items);

    boolean isStockDecreased(UUID orderId);
    boolean isStockRestored(UUID orderId);

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
