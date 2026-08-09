package com.logistics.companyproductservice.application.dto;

import com.logistics.companyproductservice.domain.model.Company;

import java.util.UUID;

public record CompanyInfo(
        UUID id,
        UUID hubId,
        String name,
        String address
) {
    public static CompanyInfo from(Company company) {
        return new CompanyInfo(
                company.getId(),
                company.getHubId(),
                company.getName(),
                company.getAddress()
        );
    }
}