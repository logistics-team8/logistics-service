package com.logistics.companyproductservice.presentation.dto.request;

import com.logistics.companyproductservice.domain.model.Company;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.UUID;

@Getter
public class CompanyCreateRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotNull
    private Company.Type type;

    @NotNull
    private UUID hubId;

    @NotBlank
    @Size(max = 255)
    private String address;
}