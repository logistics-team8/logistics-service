package com.logistics.companyproductservice.presentation.controller;

import com.logistics.companyproductservice.application.dto.CompanyInfo;
import com.logistics.companyproductservice.application.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/companies")
public class InternalCompanyController {

    private final CompanyService companyService;

    @GetMapping("/{companyId}")
    public CompanyInfo getCompanyInfo(@PathVariable UUID companyId) {
        return companyService.getCompanyInfo(companyId);
    }
}