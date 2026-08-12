package com.logistics.companyproductservice.application.service;

import com.logistics.companyproductservice.domain.model.StockTransaction;
import com.logistics.companyproductservice.domain.model.StockTransactionType;
import com.logistics.companyproductservice.domain.repository.StockTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StockTransactionService {

    private final StockTransactionRepository stockTransactionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryClaim(UUID orderId, StockTransactionType type) {
        try {
            stockTransactionRepository.saveAndFlush(StockTransaction.create(orderId, type));
            return true;
        } catch (DataIntegrityViolationException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
    }
}