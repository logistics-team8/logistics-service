package com.logistics.companyproductservice.application.service;

import com.logistics.companyproductservice.domain.model.StockTransactionType;
import com.logistics.companyproductservice.domain.repository.StockTransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockTransactionService")
class StockTransactionServiceTest {

    @Mock
    private StockTransactionRepository stockTransactionRepository;

    @InjectMocks
    private StockTransactionService stockTransactionService;

    @Test
    @DisplayName("처음 들어온 orderId+type이면 선점에 성공하고 true를 반환한다")
    void returnsTrueWhenFirstClaim() {
        UUID orderId = UUID.randomUUID();
        when(stockTransactionRepository.saveAndFlush(any())).thenReturn(null);

        boolean result = stockTransactionService.tryClaim(orderId, StockTransactionType.DECREASE);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("이미 선점된 orderId+type이면 false를 반환하고 트랜잭션을 롤백 처리한다")
    void returnsFalseWhenAlreadyClaimed() {
        UUID orderId = UUID.randomUUID();
        doThrow(new DataIntegrityViolationException("duplicate"))
                .when(stockTransactionRepository).saveAndFlush(any());

        TransactionStatus transactionStatus = mock(TransactionStatus.class);
        try (MockedStatic<TransactionAspectSupport> mockedStatic = mockStatic(TransactionAspectSupport.class)) {
            mockedStatic.when(TransactionAspectSupport::currentTransactionStatus).thenReturn(transactionStatus);

            boolean result = stockTransactionService.tryClaim(orderId, StockTransactionType.RESTORE);

            assertThat(result).isFalse();
            verify(transactionStatus).setRollbackOnly();
        }
    }
}