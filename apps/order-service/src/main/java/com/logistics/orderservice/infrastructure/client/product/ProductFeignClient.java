package com.logistics.orderservice.infrastructure.client.product;

import com.logistics.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.security.core.parameters.P;
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
    List<ProductResponse> getProducts(  @RequestParam("ids") List<UUID> productIds);

    @PatchMapping("/decrease-stock")
    ApiResponse<?> decreaseStock(@RequestBody StockItemListRequest request);

    @PatchMapping("/restore-stock")
    ApiResponse<?> restoreStock(@RequestBody StockItemListRequest request);

    record ProductResponse(
            UUID id,
            String name,
            UUID companyId,
            UUID hubId
    ){
    }

    record StockItemListRequest(
            List<StockItemRequest> items
    ){
    }

    record StockItemRequest(
            UUID productId,
            Integer quantity
    ){
    }

}
