package com.logistics.companyproductservice.presentation.controller;

import com.logistics.common.response.ApiResponse;
import com.logistics.companyproductservice.application.service.CompanyService;
import com.logistics.companyproductservice.domain.model.Company;
import com.logistics.companyproductservice.presentation.dto.request.CompanyCreateRequest;
import com.logistics.companyproductservice.presentation.dto.response.CompanyResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}