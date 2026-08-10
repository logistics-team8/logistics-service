package com.logistics.notificationservice.application.slack;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlackRetryScheduler {

    private final SlackMessageService slackMessageService;

    @Scheduled(fixedDelay = 60000)
    public void retryFailedMessages() {

        try {
            slackMessageService.retryFailedMessages();
        } catch (Exception e) {
            log.error("Slack 메시지 재전송 처리 실패", e);
        }
    }
}