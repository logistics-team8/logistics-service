package com.logistics.notificationservice.infrastructure.persistence;

import com.logistics.notificationservice.domain.ai.AiRequestLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AiRequestLogJpaRepository extends JpaRepository<AiRequestLog, UUID> {


}
