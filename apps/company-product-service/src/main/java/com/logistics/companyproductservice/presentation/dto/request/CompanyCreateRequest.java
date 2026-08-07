package com.logistics.companyproductservice.presentation.dto.request;

import com.logistics.companyproductservice.domain.model.Company;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.UUID;

@Getter
public class CompanyCreateRequest {

    @NotBlank
    private String name;

    @NotNull
    private Company.Type type;

    @NotNull
    private UUID hubId;

    @NotBlank
    private String address;
}