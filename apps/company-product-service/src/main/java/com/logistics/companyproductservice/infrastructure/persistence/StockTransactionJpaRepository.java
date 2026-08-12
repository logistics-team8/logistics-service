package com.logistics.companyproductservice.infrastructure.persistence;

import com.logistics.companyproductservice.domain.model.StockTransaction;
import com.logistics.companyproductservice.domain.model.StockTransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StockTransactionJpaRepository extends JpaRepository<StockTransaction, UUID> {
    Optional<StockTransaction> findByOrderIdAndType(UUID orderId, StockTransactionType type);
}