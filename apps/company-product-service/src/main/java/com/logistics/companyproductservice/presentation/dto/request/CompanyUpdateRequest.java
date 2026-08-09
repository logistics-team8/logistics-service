package com.logistics.companyproductservice.presentation.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class CompanyUpdateRequest {

    @Size(max = 100)
    private String name;

    @Size(max = 255)
    private String address;
}