package com.logistics.companyproductservice.presentation.controller;

import com.logistics.common.response.ApiResponse;
import com.logistics.common.security.principal.CustomUserDetails;
import com.logistics.companyproductservice.application.page.PageResponse;
import com.logistics.companyproductservice.application.service.ProductService;
import com.logistics.companyproductservice.domain.model.Product;
import com.logistics.companyproductservice.presentation.dto.request.ProductCreateRequest;
import com.logistics.companyproductservice.presentation.dto.request.ProductUpdateRequest;
import com.logistics.companyproductservice.presentation.dto.response.ProductResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@RequestBody @Valid ProductCreateRequest request) {
        Product product = productService.create(request);
        ProductResponse response = ProductResponse.from(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> getProduct(@PathVariable UUID id) {
        Product product = productService.getProduct(id);
        return ApiResponse.success(ProductResponse.from(product));
    }

    @PatchMapping("/{id}")
    public ApiResponse<ProductResponse> updateProduct(@PathVariable UUID id, @RequestBody @Valid ProductUpdateRequest request) {
        Product product = productService.update(id, request);
        return ApiResponse.success(ProductResponse.from(product));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteProduct(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        productService.delete(id, userDetails.getId());
        return ApiResponse.success(null);
    }

    @GetMapping
    public ApiResponse<PageResponse<ProductResponse>> getProducts(
            @RequestParam(required = false) String name,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ProductResponse> page = productService.search(name, pageable);
        return ApiResponse.success(PageResponse.from(page));
    }
}