package com.logistics.notificationservice.domain.slack;

import com.logistics.notificationservice.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;
@Entity
@Table(name = "p_slack_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SlackMessage extends BaseEntity {

    private static final int MAX_ATTEMPT_COUNT = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name= "slack_message_id")
    private UUID slackMessageId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    /** ai 요청 id **/
    @Column(name = "ai_request_id", nullable = false)
    private UUID aiRequestId;

    /** 수신자 id **/
    @Column(name = "recipient_user_id")
    private UUID recipientUserId;

    /** 슬랙 id **/
    @Column(name = "recipient_slack_id")
    private String recipientSlackId;


    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    /** 발송 상태 PENDING,SENT,FAILED **/
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SlackMessageStatus status;

    /** 재시도 횟수 **/
    @Column(name = "retry_count")
    private Integer retryCount;

    /** 실패 사유 **/
    @Column(name = "failure_reason")
    private String failureReason;

    /** 발송 시간 **/
    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    /** 초기값 세팅 **/
    private SlackMessage(
            UUID orderId,
            UUID aiRequestId,
            UUID recipientUserId,
            String recipientSlackId,
            String message
    ) {
        this.orderId = orderId;
        this.aiRequestId = aiRequestId;
        this.recipientUserId = recipientUserId;
        this.recipientSlackId = recipientSlackId;
        this.message = message;
        this.status = SlackMessageStatus.PENDING;
        this.retryCount = 0;
    }


    public static SlackMessage create(
            UUID orderId,
            UUID aiRequestId,
            UUID recipientUserId,
            String recipientSlackId,
            String message
    ) {
        return new SlackMessage(
                orderId,
                aiRequestId,
                recipientUserId,
                recipientSlackId,
                message
        );
    }
    public void markAsSent(){
        if (isComplete()){
            throw new IllegalStateException("이미 처리가 종료된 Slack 메시지 입니다.");
        }

        this.status = SlackMessageStatus.SUCCESS;
        this.sentAt = LocalDateTime.now();
        this.failureReason = null;
    }

    public void markAsFailure(String failureReason){
        if(isComplete()){
            throw  new IllegalStateException("이미 처리가 종료된 Slack 메시지 입니다.");
        }
        this.retryCount ++;
        this.failureReason = failureReason;

        if(this.retryCount >= MAX_ATTEMPT_COUNT){
            this.status = SlackMessageStatus.FAILED;
            return;
        }
        this.status = SlackMessageStatus.PENDING;
    }



    public boolean canRetry(){
        return this.status == SlackMessageStatus.PENDING && this.retryCount < MAX_ATTEMPT_COUNT;
    }



    public boolean isComplete() {
        //pending 상태가 아니면 처리완료 상태
        return status == SlackMessageStatus.SUCCESS || this.status == SlackMessageStatus.FAILED;
    }



}
