package com.logistics.companyproductservice.presentation.controller;

import com.logistics.common.response.ApiResponse;
import com.logistics.companyproductservice.application.service.ProductService;
import com.logistics.companyproductservice.presentation.dto.request.StockAdjustRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}