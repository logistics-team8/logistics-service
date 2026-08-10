package com.logistics.companyproductservice.presentation.controller;

import com.logistics.common.response.ApiResponse;
import com.logistics.companyproductservice.application.dto.CompanyInfo;
import com.logistics.companyproductservice.application.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/companies")
public class InternalCompanyController {

    private final CompanyService companyService;

    @GetMapping("/{companyId}")
    public ApiResponse<CompanyInfo> getCompanyInfo(@PathVariable UUID companyId) {
        return ApiResponse.success(companyService.getCompanyInfo(companyId));
    }

    @GetMapping("/batch")
    public ApiResponse<List<CompanyInfo>> getCompanyInfos(@RequestParam List<UUID> ids) {
        return ApiResponse.success(companyService.getCompanyInfos(ids));
    }
}