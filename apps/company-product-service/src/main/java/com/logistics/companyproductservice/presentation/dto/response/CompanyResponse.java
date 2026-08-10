package com.logistics.companyproductservice.presentation.dto.response;

import com.logistics.companyproductservice.domain.model.Company;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CompanyResponse {

    private UUID id;
    private String name;
    private Company.Type type;
    private UUID hubId;
    private String address;

    public static CompanyResponse from(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getType(),
                company.getHubId(),
                company.getAddress()
        );
    }
}