package com.logistics.companyproductservice.presentation.controller;

import com.logistics.common.response.ApiResponse;
import com.logistics.companyproductservice.application.page.PageResponse;
import com.logistics.companyproductservice.application.service.CompanyService;
import com.logistics.companyproductservice.domain.model.Company;
import com.logistics.companyproductservice.presentation.dto.request.CompanyCreateRequest;
import com.logistics.companyproductservice.presentation.dto.request.CompanyUpdateRequest;
import com.logistics.companyproductservice.presentation.dto.response.CompanyResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping
    public ResponseEntity<ApiResponse<CompanyResponse>> createCompany(@RequestBody @Valid CompanyCreateRequest request) {
        Company company = companyService.create(request);
        CompanyResponse response = CompanyResponse.from(company);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }
    @GetMapping("/{id}")
    public ApiResponse<CompanyResponse> getCompany(@PathVariable UUID id) {
        Company company = companyService.getCompany(id);
        return ApiResponse.success(CompanyResponse.from(company));
    }
    @PatchMapping("/{id}")
    public ApiResponse<CompanyResponse> updateCompany(@PathVariable UUID id, @RequestBody @Valid CompanyUpdateRequest request) {
        Company company = companyService.update(id, request);
        return ApiResponse.success(CompanyResponse.from(company));
    }
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteCompany(@PathVariable UUID id, @RequestHeader("X-User-Id") UUID userId) {
        companyService.delete(id, userId);
        return ApiResponse.success(null);
    }
    @GetMapping
    public ApiResponse<PageResponse<CompanyResponse>> getCompanies(
            @RequestParam(required = false) String name,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<CompanyResponse> page = companyService.search(name, pageable);
        return ApiResponse.success(PageResponse.from(page));
    }
}