package com.logistics.notificationservice.infrastructure.slack;

import com.logistics.notificationservice.application.slack.SlackClient;
import com.logistics.notificationservice.domain.common.exception.NotificationErrorCode;
import com.logistics.notificationservice.domain.common.exception.NotificationException;
import com.logistics.notificationservice.infrastructure.slack.dto.SlackApiRequestDto;
import com.logistics.notificationservice.infrastructure.slack.dto.SlackApiResponseDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class SlackClientImpl implements SlackClient {
    private final RestClient slackRestClient;
    private final SlackProperties slackProperties;

    public SlackClientImpl(
            @Qualifier("slackRestClient") RestClient slackRestClient,
            SlackProperties slackProperties
    ) {
        this.slackRestClient = slackRestClient;
        this.slackProperties = slackProperties;
    }


    @Override
    public void sendMessage(String slackUserId, String message) {

        SlackApiRequestDto request = new SlackApiRequestDto(slackUserId, slackProperties.getChannelId(), message);

        SlackApiResponseDto response =
                slackRestClient
                        .post()
                        .uri("/chat.postMessage")
                        .body(request)
                        .retrieve()
                        .body(SlackApiResponseDto.class);

        if (response == null) {
            throw new NotificationException(NotificationErrorCode.SLACK_RESPONSE_EMPTY);
        }

        if (!response.isOk()) {
            throw new NotificationException(NotificationErrorCode.SLACK_SEND_FAILED);
        }
    }
}
