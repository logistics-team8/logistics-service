package com.logistics.companyproductservice.presentation.controller;

import com.logistics.common.response.ApiResponse;
import com.logistics.companyproductservice.application.service.ProductService;
import com.logistics.companyproductservice.domain.model.Product;
import com.logistics.companyproductservice.presentation.dto.request.ProductCreateRequest;
import com.logistics.companyproductservice.presentation.dto.response.ProductResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ApiResponse<ProductResponse> createProduct(@RequestBody @Valid ProductCreateRequest request) {
        Product product = productService.create(request);
        ProductResponse response = ProductResponse.from(product);
        return ApiResponse.success(response);
    }
}