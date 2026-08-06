package com.logistics.notificationservice.infrastructure.persistence;

import com.logistics.notificationservice.domain.ai.AiRequestLog;
import com.logistics.notificationservice.domain.ai.AiRequestLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AiRequestLogRepositoryImpl implements AiRequestLogRepository {
    private final AiRequestLogJpaRepository jpaRepository;


    public AiRequestLog save(AiRequestLog aiRequestLog) {
        //리포지토리 구현체
        return jpaRepository.save(aiRequestLog);
    }



}
