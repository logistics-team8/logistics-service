package com.logistics.companyproductservice.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(
        name = "p_stock_transactions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_stock_transaction_order_type",
                columnNames = {"order_id", "type"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StockTransactionType type;

    private StockTransaction(UUID orderId, StockTransactionType type) {
        this.orderId = orderId;
        this.type = type;
    }

    public static StockTransaction create(UUID orderId, StockTransactionType type) {
        return new StockTransaction(orderId, type);
    }
}