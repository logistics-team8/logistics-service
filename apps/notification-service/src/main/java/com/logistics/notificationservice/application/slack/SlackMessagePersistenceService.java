package com.logistics.notificationservice.application.slack;

import com.logistics.notificationservice.domain.slack.SlackMessage;
import com.logistics.notificationservice.domain.slack.SlackMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;


@Service
@RequiredArgsConstructor
public class SlackMessagePersistenceService {


    private final SlackMessageRepository slackMessageRepository;

    /**
     * 최초 메시지 PENDING 상태 저장
     */
    @Transactional
    public SlackMessage create(
            UUID orderId,
            UUID aiRequestId,
            UUID recipientUserId,
            String recipientSlackId,
            String message
    ) {

        SlackMessage slackMessage =
                SlackMessage.create(
                        orderId,
                        aiRequestId,
                        recipientUserId,
                        recipientSlackId,
                        message
                );

        return slackMessageRepository.save(slackMessage);
    }

    /**
     * Slack 메시지 상태 변경 저장
     */
    @Transactional
    public SlackMessage save(
            SlackMessage slackMessage
    ) {
        return slackMessageRepository.save(slackMessage);
    }
}
