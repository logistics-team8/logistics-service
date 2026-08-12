package com.logistics.companyproductservice.domain.repository;

import com.logistics.companyproductservice.domain.model.StockTransaction;
import com.logistics.companyproductservice.domain.model.StockTransactionType;

import java.util.Optional;
import java.util.UUID;

public interface StockTransactionRepository {
    StockTransaction saveAndFlush(StockTransaction transaction);
    Optional<StockTransaction> findByOrderIdAndType(UUID orderId, StockTransactionType type);
}