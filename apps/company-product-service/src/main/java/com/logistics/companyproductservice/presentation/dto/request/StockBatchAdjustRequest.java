package com.logistics.companyproductservice.presentation.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
public class StockBatchAdjustRequest {

    @NotEmpty
    @Valid
    private List<Item> items;

    @Getter
    public static class Item {

        @NotNull
        private UUID productId;

        @NotNull
        @Positive
        private Integer quantity;
    }
}