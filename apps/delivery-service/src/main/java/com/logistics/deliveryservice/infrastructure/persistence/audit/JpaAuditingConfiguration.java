package com.logistics.deliveryservice.infrastructure.persistence.audit;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Delivery Service Entity의 생성·수정 시각 감사를 활성화한다.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfiguration {
}
