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

     private static final UUID SYSTEM_USER_ID =
             UUID.fromString("00000000-0000-0000-0000-000000000001");

     @Id
     @GeneratedValue(strategy = GenerationType.UUID)
     @Column(name = "ai_request_id")
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
     private Long responseTimeMs;

     @Enumerated(EnumType.STRING)
     @Column(nullable = false, length = 20)
     private AiRequestStatus status;

     @Column(name = "failure_reason", columnDefinition = "TEXT")
     private String failureReason;

     @Column(name = "requested_at")
     private LocalDateTime requestedAt;

     @Column(name = "responded_at")
     private LocalDateTime respondedAt;

     private AiRequestLog(
             UUID orderId,
             String prompt
     ) {
          this.orderId = orderId;
          this.prompt = prompt;
          this.requestedAt = LocalDateTime.now();
          this.status = AiRequestStatus.PROCESSING;
          setCreatedBy(SYSTEM_USER_ID);
     }

     public static AiRequestLog create(
             UUID orderId,
             String prompt
     ) {
          return new AiRequestLog(orderId, prompt);
     }

     public void success(
          String response,
          LocalDateTime finalDispatchDeadline,
          String modelName,
          Long responseTimeMs
     ){

          this.response = response;
          this.finalDispatchDeadline = finalDispatchDeadline;
          this.modelName = modelName;
          this.responseTimeMs = (long) Math.toIntExact(responseTimeMs);
          this.status = AiRequestStatus.SUCCESS;
          this.respondedAt = LocalDateTime.now();
     }

     public void fail(String failureReason) {
          this.status = AiRequestStatus.FAILED;
          this.failureReason = failureReason;
          this.respondedAt = LocalDateTime.now();
     }

}
