package com.logistics.companyproductservice.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Entity
@Table(name = "p_products")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private UUID companyId;

    @Column(nullable = false)
    private UUID hubId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private Integer stockQuantity;

    private Product(String name, UUID companyId, UUID hubId, BigDecimal unitPrice) {
        this.name = name;
        this.companyId = companyId;
        this.hubId = hubId;
        this.unitPrice = unitPrice;
        this.stockQuantity = 0;
    }

    public static Product create(String name, UUID companyId, UUID hubId, BigDecimal unitPrice) {
        return new Product(name, companyId, hubId, unitPrice);
    }
}