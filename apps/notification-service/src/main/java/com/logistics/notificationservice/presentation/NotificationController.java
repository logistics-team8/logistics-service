package com.logistics.notificationservice.presentation;

import com.logistics.common.response.ApiResponse;
import com.logistics.notificationservice.application.notificaion.OrderNotificationService;
import com.logistics.notificationservice.application.slack.SlackMessageService;
import com.logistics.notificationservice.presentation.slack.dto.OrderNotificationRequestDto;
import com.logistics.notificationservice.presentation.slack.dto.SlackMessageRequestDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/slack/messages")
public class NotificationController {

    private final SlackMessageService slackMessageService;
    private final OrderNotificationService orderNotificationService;



    @PostMapping("/orders")
    public ResponseEntity<ApiResponse<Void>> notifyOrderCreated(
            @RequestBody OrderNotificationRequestDto request
    ) {

        orderNotificationService.notifyOrderCreated(request);

        return ResponseEntity
                .accepted()
                .body(ApiResponse.success(null));
    }
}
