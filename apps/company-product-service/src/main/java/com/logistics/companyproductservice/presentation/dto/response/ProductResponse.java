package com.logistics.companyproductservice.presentation.dto.response;

import com.logistics.companyproductservice.domain.model.Product;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ProductResponse {

    private UUID id;
    private String name;
    private UUID companyId;
    private UUID hubId;
    private BigDecimal unitPrice;
    private Integer stockQuantity;

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getCompanyId(),
                product.getHubId(),
                product.getUnitPrice(),
                product.getStockQuantity()
        );
    }
}