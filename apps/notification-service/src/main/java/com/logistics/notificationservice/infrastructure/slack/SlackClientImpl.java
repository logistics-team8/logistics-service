package com.logistics.notificationservice.infrastructure.slack;

import com.logistics.notificationservice.application.slack.SlackClient;
import com.logistics.notificationservice.infrastructure.slack.dto.SlackRequest;
import com.logistics.notificationservice.infrastructure.slack.dto.SlackResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class SlackClientImpl implements SlackClient {
    private final RestClient slackRestClient;
    private final SlackProperties slackProperties;

    @Override
    public void sendMessage(String slackUserId, String message) {

        SlackRequest request = new SlackRequest(slackUserId, slackProperties.getChannelId(),message);

        SlackResponse response =
                slackRestClient
                        .post()
                        .uri("/chat.postMessage")
                        .body(request)
                        .retrieve()
                        .body(SlackResponse.class);

        if (response == null) {
            throw new IllegalStateException("Slack 응답이 없습니다.");
        }

        if (!response.isOk()) {
            throw new IllegalStateException(
                    "Slack API 오류: " + response.getErrorCode()
            );
        }

    }
}
