package com.logistics.companyproductservice.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class ProductUpdateRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @Positive
    private BigDecimal unitPrice;
}