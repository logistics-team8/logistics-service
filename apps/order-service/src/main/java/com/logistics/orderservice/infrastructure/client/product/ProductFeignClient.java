package com.logistics.orderservice.infrastructure.client.product;

import com.logistics.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@FeignClient(
        name = "company-product-service",
        contextId = "productFeignClient",
        path = "/internal/v1/products"
)
public interface ProductFeignClient {

    @GetMapping("/batch")
    ApiResponse<List<ProductResponse>> getProducts(  @RequestParam("ids") List<UUID> productIds);

    @PatchMapping("/decrease-stock")
    ApiResponse<?> decreaseStock(@RequestBody StockItemListRequest request);

    @PatchMapping("/restore-stock")
    ApiResponse<?> restoreStock(@RequestBody StockItemListRequest request);

    @GetMapping("/decrease-stock/{orderId}")
    ApiResponse<Boolean> isStockDecreased(@PathVariable UUID orderId);

    @GetMapping("/restore-stock/{orderId}")
    ApiResponse<Boolean> isStockRestored(@PathVariable UUID orderId);

    record ProductResponse(
            UUID id,
            String name,
            UUID companyId,
            UUID hubId
    ){
    }

    record StockItemListRequest(
            UUID orderId,
            List<StockItemRequest> items
    ){
    }

    record StockItemRequest(
            UUID productId,
            Integer quantity
    ){
    }

}
