package com.logistics.notificationservice.domain.ai;

import com.logistics.notificationservice.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "p_ai_request_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiRequestLog extends BaseEntity {

     @Id
     @GeneratedValue(strategy = GenerationType.UUID)
     @Column
     private UUID aiRequestId;

     @Column(name = "order_id", nullable = false)
     private UUID orderId;

     @Column(columnDefinition = "TEXT")
     private String prompt;

     @Column(columnDefinition = "TEXT")
     private String response;

     @Column(name = "final_dispatch_deadline")
     private LocalDateTime finalDispatchDeadline;

     @Column(name = "model_name", length = 100)
     private String modelName;

     @Column(name = "response_time_ms")
     private Integer responseTimeMs;

     @Enumerated(EnumType.STRING)
     @Column(nullable = false, length = 20)
     private AiRequestStatus status;

     @Column(name = "failure_reason", columnDefinition = "TEXT")
     private String failureReason;

     @Column(name = "requested_at")
     private LocalDateTime requestedAt;

     @Column(name = "responded_at")
     private LocalDateTime respondedAt;

}
