package com.logistics.companyproductservice.application.dto;

import com.logistics.companyproductservice.domain.model.Product;

import java.util.UUID;

public record ProductInfo(
        UUID id,
        String name,
        UUID companyId,
        UUID hubId
) {
    public static ProductInfo from(Product product) {
        return new ProductInfo(
                product.getId(),
                product.getName(),
                product.getCompanyId(),
                product.getHubId()
        );
    }
}