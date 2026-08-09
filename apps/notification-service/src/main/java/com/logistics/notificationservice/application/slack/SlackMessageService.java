package com.logistics.notificationservice.application.slack;

import com.logistics.notificationservice.domain.slack.SlackMessage;
import com.logistics.notificationservice.infrastructure.persistence.SlackMessageJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SlackMessageService {

    private final SlackClient slackClient;
    private final SlackMessageJpaRepository slackMessageRepository;

    /**
     * 최초 Slack 메시지 전송
     * **/

    @Transactional
    public void sendMessage(
            UUID orderId,
            UUID aiRequestId,
            UUID recipientUserId,
            String recipientSlackId,
            String message
    ){
        SlackMessage slackMessage = SlackMessage.create(
                orderId,
                aiRequestId,
                recipientUserId,
                recipientSlackId,
                message
        );

        slackMessageRepository.save(slackMessage);
        send(slackMessage);
    }


    private void send (SlackMessage slackMessage){
        try{
            slackClient.sendMessage(
                    slackMessage.getRecipientSlackId(),
                    slackMessage.getMessage()
            );
            slackMessage.markAsSent();
        }catch (Exception e){
            String failureReason = extractFailureReason(e);
            slackMessage.markAsFailure(failureReason);

            throw new IllegalStateException(
                    "Slack 메시지 전송에 실패했습니다: " + failureReason,e);
        }
    }

    private String extractFailureReason(Exception e){
        return e.getMessage() != null
                ? e.getMessage()
                : e.getClass().getSimpleName();
    }


}
