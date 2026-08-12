package com.logistics.notificationservice.application.slack;

import com.logistics.notificationservice.domain.common.exception.NotificationErrorCode;
import com.logistics.notificationservice.domain.common.exception.NotificationException;
import com.logistics.notificationservice.domain.slack.SlackMessage;
import com.logistics.notificationservice.domain.slack.SlackMessageRepository;
import com.logistics.notificationservice.presentation.slack.dto.SlackMessageResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SlackMessageService {

    private final SlackClient slackClient;
    private final SlackMessageRepository slackMessageRepository;
    private final SlackMessagePersistenceService persistenceService;

    /**
     * 최초 Slack 메시지 전송
     */
    public void sendMessage(
            UUID orderId,
            UUID aiRequestId,
            UUID recipientUserId,
            String recipientSlackId,
            String message
    ) {

        SlackMessage slackMessage =
                persistenceService.create(
                        orderId,
                        aiRequestId,
                        recipientUserId,
                        recipientSlackId,
                        message
                );

        try {

            slackClient.sendMessage(
                    slackMessage.getRecipientSlackId(),
                    slackMessage.getMessage()
            );

            slackMessage.markAsSent();

            persistenceService.save(
                    slackMessage
            );

        } catch (Exception e) {

            String failureReason =
                    extractFailureReason(e);

            slackMessage.markAsFailure(
                    failureReason
            );

            persistenceService.save(
                    slackMessage
            );

            throw new IllegalStateException(
                    "Slack 메시지 전송에 실패했습니다: "
                            + failureReason,
                    e
            );
        }
    }

    @Transactional(readOnly = true)
    public SlackMessageResponseDto getSlackMessage(
            UUID slackMessageId
    ) {

        SlackMessage slackMessage = slackMessageRepository
                        .findById(slackMessageId)
                        .orElseThrow(() ->
                                new NotificationException(NotificationErrorCode.SLACK_MESSAGE_NOT_FOUND));

        return SlackMessageResponseDto.from(
                slackMessage
        );
    }

    /** Slack Message 전체 조회 **/
    @Transactional(readOnly = true)
    public Page<SlackMessageResponseDto> getSlackMessages(
            SlackMessageSearchCondition condition,
            Pageable pageable
    ) {

        return slackMessageRepository
                .search(condition, pageable)
                .map(SlackMessageResponseDto::from);
    }

    /**
     * Scheduler에서 호출
     */
    public void retryFailedMessages() {

        List<SlackMessage> messages =
                slackMessageRepository.findRetryTargets();

        for (SlackMessage message : messages) {

            retry(message);
        }
    }


    private void retry(
            SlackMessage message
    ) {

        if (!message.canRetry()) {
            return;
        }

        try {

            slackClient.sendMessage(
                    message.getRecipientSlackId(),
                    message.getMessage()
            );

            message.markAsSent();

            persistenceService.save(message);

        } catch (Exception e) {

            String failureReason =
                    extractFailureReason(e);

            message.markAsFailure(
                    failureReason
            );

            persistenceService.save(message);
        }
    }


    private String extractFailureReason(
            Exception e
    ) {

        return e.getMessage() != null
                ? e.getMessage()
                : e.getClass().getSimpleName();
    }
}