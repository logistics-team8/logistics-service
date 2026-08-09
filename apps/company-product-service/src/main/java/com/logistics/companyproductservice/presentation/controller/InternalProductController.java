package com.logistics.companyproductservice.presentation.controller;

import com.logistics.common.response.ApiResponse;
import com.logistics.companyproductservice.application.dto.ProductInfo;
import com.logistics.companyproductservice.application.service.ProductService;
import com.logistics.companyproductservice.presentation.dto.request.StockAdjustRequest;
import com.logistics.companyproductservice.presentation.dto.request.StockBatchAdjustRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/products")
public class InternalProductController {

    private final ProductService productService;

    @PatchMapping("/{id}/decrease-stock")
    public ApiResponse<Void> decreaseStock(@PathVariable UUID id, @RequestBody @Valid StockAdjustRequest request) {
        productService.decreaseStock(id, request.getQuantity());
        return ApiResponse.success(null);
    }

    @PatchMapping("/{id}/restore-stock")
    public ApiResponse<Void> restoreStock(@PathVariable UUID id, @RequestBody @Valid StockAdjustRequest request) {
        productService.restoreStock(id, request.getQuantity());
        return ApiResponse.success(null);
    }
    @GetMapping("/{productId}")
    public ProductInfo getProductInfo(@PathVariable UUID productId) {
        return productService.getProductInfo(productId);
    }

    @PatchMapping("/decrease-stock")
    public ApiResponse<Void> decreaseStockBatch(@RequestBody @Valid StockBatchAdjustRequest request) {
        productService.decreaseStockBatch(request);
        return ApiResponse.success(null);
    }

    @PatchMapping("/restore-stock")
    public ApiResponse<Void> restoreStockBatch(@RequestBody @Valid StockBatchAdjustRequest request) {
        productService.restoreStockBatch(request);
        return ApiResponse.success(null);
    }
    @GetMapping("/batch")
    public List<ProductInfo> getProductInfos(@RequestParam List<UUID> ids) {
        return productService.getProductInfos(ids);
    }
}