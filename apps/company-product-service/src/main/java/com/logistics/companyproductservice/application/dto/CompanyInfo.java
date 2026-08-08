package com.logistics.companyproductservice.application.dto;

import com.logistics.companyproductservice.domain.model.Company;

import java.util.UUID;

public record CompanyInfo(
        UUID id,
        String name,
        Company.Type type,
        UUID hubId
) {
    public static CompanyInfo from(Company company) {
        return new CompanyInfo(
                company.getId(),
                company.getName(),
                company.getType(),
                company.getHubId()
        );
    }
}