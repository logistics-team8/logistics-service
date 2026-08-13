package com.logistics.companyproductservice.presentation.controller;

import com.logistics.common.response.ApiResponse;
import com.logistics.companyproductservice.application.dto.ProductInfo;
import com.logistics.companyproductservice.application.service.ProductService;
import com.logistics.companyproductservice.presentation.dto.request.StockAdjustRequest;
import com.logistics.companyproductservice.presentation.dto.request.StockBatchAdjustRequest;
import com.logistics.companyproductservice.domain.model.StockTransactionType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/products")
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
    public ApiResponse<ProductInfo> getProductInfo(@PathVariable UUID productId) {
        return ApiResponse.success(productService.getProductInfo(productId));
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
    public ApiResponse<List<ProductInfo>> getProductInfos(@RequestParam List<UUID> ids) {
        return ApiResponse.success(productService.getProductInfos(ids));
    }
    @GetMapping("/decrease-stock/{orderId}")
    public ApiResponse<Boolean> getDecreaseStockStatus(@PathVariable UUID orderId) {
        return ApiResponse.success(productService.isStockTransactionProcessed(orderId, StockTransactionType.DECREASE));
    }

    @GetMapping("/restore-stock/{orderId}")
    public ApiResponse<Boolean> getRestoreStockStatus(@PathVariable UUID orderId) {
        return ApiResponse.success(productService.isStockTransactionProcessed(orderId, StockTransactionType.RESTORE));
    }
}