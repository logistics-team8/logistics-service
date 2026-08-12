package com.logistics.notificationservice.application.notificaion;


import com.logistics.notificationservice.application.ai.AiDispatchCommand;
import com.logistics.notificationservice.application.ai.AiDispatchProcessResult;
import com.logistics.notificationservice.application.ai.AiDispatchService;
import com.logistics.notificationservice.application.ai.SlackMessageGenerator;
import com.logistics.notificationservice.application.slack.SlackMessageService;
import com.logistics.notificationservice.application.user.UserClient;
import com.logistics.notificationservice.presentation.slack.dto.DispatchNotificationRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderNotificationEventListener {

    private final AiDispatchService aiDispatchService;
    private final SlackMessageGenerator slackMessageGenerator;
    private final SlackMessageService slackMessageService;
    private final UserClient userClient;

    @Async
    @EventListener
    public void handle(OrderNotificationEvent event) {

        DispatchNotificationRequestDto request = event.request();

        try {
            AiDispatchCommand command =
                    AiDispatchCommand.from(request);

            AiDispatchProcessResult aiResult =
                    aiDispatchService.calculateDeadline(command);


            String message =
                    slackMessageGenerator.generate(
                            command,
                            aiResult.result()
                    );

            String slackId =
                    userClient.getSlackId(
                            request.recipientUserId()
                    );


            slackMessageService.sendMessage(
                    command.orderId(),
                    aiResult.aiRequestId(),
                    request.recipientUserId(),          // DB 저장용
                    slackId,  // Slack 전송용
                    message
            );

        } catch (Exception e) {
            log.error(
                    "주문 알림 비동기 처리 실패. orderId={}",
                    request.orderId(),
                    e
            );
        }
    }
}