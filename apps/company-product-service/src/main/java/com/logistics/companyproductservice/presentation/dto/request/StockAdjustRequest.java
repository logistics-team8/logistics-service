package com.logistics.companyproductservice.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

@Getter
public class StockAdjustRequest {

    @NotNull
    @Positive
    private Integer quantity;
}