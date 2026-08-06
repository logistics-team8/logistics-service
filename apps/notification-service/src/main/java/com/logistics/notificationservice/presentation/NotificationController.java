package com.logistics.notificationservice.presentation;

import com.logistics.notificationservice.application.slack.SlackMessageService;
import com.logistics.notificationservice.presentation.slack.dto.SlackMessageRequestDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/slack/messages")
public class NotificationController {

    private final SlackMessageService slackMessageService;


    // TODO: 임시 Slack Message 전송 테스트 & 개인DM or 슬랙 채널로 보낼 로직 추 후 구현 예정
    @PostMapping
    public String sendMessage(
            @Valid @RequestBody SlackMessageRequestDto request
            ){
                slackMessageService.sendMessage(
                        // HACK : 기능 미 구현으로 하드코딩 적용하였습니다.
                        UUID.fromString("ed189c28-aa7c-482d-b209-302094026ef7"),
                        UUID.fromString("25ad0075-c23f-4575-bede-3b534b2605c7"),
                        UUID.fromString("7aff33ce-892d-42e9-a42a-96b947451336"),
                                "slackId",
                        request.text());
            return "ok";
    }

}
