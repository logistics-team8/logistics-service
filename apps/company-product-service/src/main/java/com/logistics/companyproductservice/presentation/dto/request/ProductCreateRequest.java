package com.logistics.companyproductservice.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
public class ProductCreateRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotNull
    private UUID companyId;

    @NotNull
    private UUID hubId;

    @NotNull
    @Positive
    private BigDecimal unitPrice;
}