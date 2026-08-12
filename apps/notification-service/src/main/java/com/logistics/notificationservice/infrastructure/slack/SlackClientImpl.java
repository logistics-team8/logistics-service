package com.logistics.notificationservice.infrastructure.slack;

import com.logistics.notificationservice.application.slack.SlackClient;
import com.logistics.notificationservice.infrastructure.slack.dto.SlackApiRequestDto;
import com.logistics.notificationservice.infrastructure.slack.dto.SlackApiResponseDto;
import com.logistics.notificationservice.infrastructure.slack.dto.SlackOpenDmRequestDto;
import com.logistics.notificationservice.infrastructure.slack.dto.SlackOpenDmResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class SlackClientImpl implements SlackClient {
    private final RestClient slackRestClient;

    public SlackClientImpl(
            @Qualifier("slackRestClient") RestClient slackRestClient,
            SlackProperties slackProperties
    ) {
        this.slackRestClient = slackRestClient;
    }


    @Override
    public void sendMessage(
            String slackUserId,
            String message
    ) {
        System.err.println("### SlackClientImpl.sendMessage 진입 ###");
        System.err.println("### slackUserId = " + slackUserId);

        log.info("Slack DM 대상 userId={}", slackUserId);



        String dmChannelId = openDmChannel(slackUserId);
        SlackApiRequestDto request = SlackApiRequestDto.of(dmChannelId, message);


        SlackApiResponseDto response =
                slackRestClient
                        .post()
                        .uri("/chat.postMessage")
                        .body(request)
                        .retrieve()
                        .body(SlackApiResponseDto.class);

        if (response == null) {
            throw new IllegalStateException(
                    "Slack 응답이 없습니다."
            );
        }

        if (!response.isOk()) {
            throw new IllegalStateException(
                    "Slack API 오류: "
                            + response.getErrorCode()
            );
        }

    }

    private String openDmChannel(
            String slackUserId
    ) {

        SlackOpenDmRequestDto request =
                new SlackOpenDmRequestDto(slackUserId);

        SlackOpenDmResponseDto response =
                slackRestClient
                        .post()
                        .uri("/conversations.open")
                        .body(request)
                        .retrieve()
                        .body(SlackOpenDmResponseDto.class);

        if (response == null) {
            throw new IllegalStateException(
                    "Slack DM 채널 응답이 없습니다."
            );
        }

        if (!response.isOk()) {
            throw new IllegalStateException(
                    "Slack DM 채널 생성 실패: "
                            + response.getErrorCode()
            );
        }

        return response.getChannel().getId();
    }
}

