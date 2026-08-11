package com.logistics.companyproductservice.infrastructure.persistence;

import com.logistics.companyproductservice.domain.model.StockTransaction;
import com.logistics.companyproductservice.domain.model.StockTransactionType;
import com.logistics.companyproductservice.domain.repository.StockTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class StockTransactionRepositoryImpl implements StockTransactionRepository {

    private final StockTransactionJpaRepository stockTransactionJpaRepository;

    @Override
    public StockTransaction saveAndFlush(StockTransaction transaction) {
        return stockTransactionJpaRepository.saveAndFlush(transaction);
    }

    @Override
    public Optional<StockTransaction> findByOrderIdAndType(UUID orderId, StockTransactionType type) {
        return stockTransactionJpaRepository.findByOrderIdAndType(orderId, type);
    }
}