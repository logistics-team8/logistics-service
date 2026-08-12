package com.logistics.notificationservice.application.slack;

import com.logistics.notificationservice.domain.common.exception.NotificationException;
import com.logistics.notificationservice.domain.slack.SlackMessage;
import com.logistics.notificationservice.domain.slack.SlackMessageRepository;
import com.logistics.notificationservice.domain.slack.SlackMessageStatus;
import com.logistics.notificationservice.presentation.slack.dto.SlackMessageResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlackMessageServiceTest {


    @Mock
    private SlackClient slackClient;

    @Mock
    private SlackMessageRepository slackMessageRepository;

    @Mock
    private SlackMessagePersistenceService persistenceService;

    private SlackMessageService slackMessageService;

    @BeforeEach
    void setUp() {
        slackMessageService = new SlackMessageService(
                slackClient,
                slackMessageRepository,
                persistenceService
        );
    }

    @Test
    void Slack_메시지_전송에_성공하면_SENT_처리한다() {

        // given
        UUID orderId = UUID.randomUUID();
        UUID aiRequestId = UUID.randomUUID();
        UUID recipientUserId = UUID.randomUUID();

        String slackId = "U123456789";
        String message = "배송 알림 테스트";

        SlackMessage slackMessage = mock(SlackMessage.class);

        when(
                persistenceService.create(
                        orderId,
                        aiRequestId,
                        recipientUserId,
                        slackId,
                        message
                )
        ).thenReturn(slackMessage);

        when(slackMessage.getRecipientSlackId())
                .thenReturn(slackId);

        when(slackMessage.getMessage())
                .thenReturn(message);

        // when
        slackMessageService.sendMessage(
                orderId,
                aiRequestId,
                recipientUserId,
                slackId,
                message
        );

        // then
        verify(slackClient)
                .sendMessage(slackId, message);

        verify(slackMessage)
                .markAsSent();

        verify(persistenceService)
                .save(slackMessage);
    }

    @Test
    void Slack_메시지_전송에_실패하면_FAILED_처리하고_예외를_던진다() {

        // given
        UUID orderId = UUID.randomUUID();
        UUID aiRequestId = UUID.randomUUID();
        UUID recipientUserId = UUID.randomUUID();

        String slackId = "U123456789";
        String message = "배송 알림 테스트";

        SlackMessage slackMessage = mock(SlackMessage.class);

        when(
                persistenceService.create(
                        orderId,
                        aiRequestId,
                        recipientUserId,
                        slackId,
                        message
                )
        ).thenReturn(slackMessage);

        when(slackMessage.getRecipientSlackId())
                .thenReturn(slackId);

        when(slackMessage.getMessage())
                .thenReturn(message);

        doThrow(
                new IllegalStateException("channel_not_found")
        ).when(slackClient)
                .sendMessage(slackId, message);

        // when
        assertThrows(
                IllegalStateException.class,
                () -> slackMessageService.sendMessage(
                        orderId,
                        aiRequestId,
                        recipientUserId,
                        slackId,
                        message
                )
        );

        // then
        verify(slackMessage)
                .markAsFailure("channel_not_found");

        verify(persistenceService)
                .save(slackMessage);
    }

    @Test
    void 실패한_Slack_메시지를_재전송하고_성공하면_SENT_처리한다() {

        // given
        SlackMessage slackMessage = mock(SlackMessage.class);

        String slackId = "U123456789";
        String message = "재전송 테스트";

        when(slackMessageRepository.findRetryTargets())
                .thenReturn(List.of(slackMessage));

        when(slackMessage.canRetry())
                .thenReturn(true);

        when(slackMessage.getRecipientSlackId())
                .thenReturn(slackId);

        when(slackMessage.getMessage())
                .thenReturn(message);

        // when
        slackMessageService.retryFailedMessages();

        // then
        verify(slackClient)
                .sendMessage(slackId, message);

        verify(slackMessage)
                .markAsSent();

        verify(persistenceService)
                .save(slackMessage);
    }

    @Test
    void 재시도_불가능한_메시지는_Slack_API를_호출하지_않는다() {

        // given
        SlackMessage slackMessage = mock(SlackMessage.class);

        when(slackMessageRepository.findRetryTargets())
                .thenReturn(List.of(slackMessage));

        when(slackMessage.canRetry())
                .thenReturn(false);

        // when
        slackMessageService.retryFailedMessages();

        // then
        verifyNoInteractions(slackClient);

        verify(persistenceService, never())
                .save(any());
    }

    @Test
    void 재시도_중_Slack_전송에_실패하면_FAILED_처리한다() {

        // given
        SlackMessage slackMessage = mock(SlackMessage.class);

        String slackId = "U123456789";
        String message = "재전송 실패 테스트";

        when(slackMessageRepository.findRetryTargets())
                .thenReturn(List.of(slackMessage));

        when(slackMessage.canRetry())
                .thenReturn(true);

        when(slackMessage.getRecipientSlackId())
                .thenReturn(slackId);

        when(slackMessage.getMessage())
                .thenReturn(message);

        doThrow(
                new IllegalStateException("timeout")
        ).when(slackClient)
                .sendMessage(slackId, message);

        // when
        slackMessageService.retryFailedMessages();

        // then
        verify(slackMessage)
                .markAsFailure("timeout");

        verify(persistenceService)
                .save(slackMessage);
    }

    @Test
    void Slack_메시지_단건_조회에_성공한다() {

        // given
        UUID slackMessageId = UUID.randomUUID();
        SlackMessage slackMessage = mock(SlackMessage.class);

        when(slackMessageRepository.findById(slackMessageId))
                .thenReturn(java.util.Optional.of(slackMessage));

        // SlackMessageResponseDto.from()에서 사용하는 getter가 있다면
        // 필요한 값들 when(...).thenReturn(...) 추가

        // when
        SlackMessageResponseDto response =
                slackMessageService.getSlackMessage(slackMessageId);

        // then
        verify(slackMessageRepository)
                .findById(slackMessageId);
    }


    @Test
    void 존재하지_않는_Slack_메시지를_조회하면_예외가_발생한다() {

        // given
        UUID slackMessageId = UUID.randomUUID();

        when(slackMessageRepository.findById(slackMessageId))
                .thenReturn(java.util.Optional.empty());

        // when & then
        assertThrows(
                NotificationException.class,
                () -> slackMessageService.getSlackMessage(slackMessageId)
        );
    }



    @Test
    void Slack_메시지_목록을_조회한다() {

        // given
        SlackMessageSearchCondition condition =
                new SlackMessageSearchCondition(
                        null,
                        null,
                        null,
                        null,
                        null
                );

        Pageable pageable =
                PageRequest.of(0, 20);

        Page<SlackMessage> page =
                Page.empty(pageable);

        when(slackMessageRepository.search(condition, pageable))
                .thenReturn(page);

        // when
        Page<SlackMessageResponseDto> result =
                slackMessageService.getSlackMessages(
                        condition,
                        pageable
                );

        // then
        verify(slackMessageRepository)
                .search(condition, pageable);

        assertEquals(0, result.getTotalElements());
    }


    @Test
    void SlackMessage_생성시_PENDING_상태이다() {

        SlackMessage message =
                SlackMessage.create(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "U123456789",
                        "테스트 메시지"
                );

        assertEquals(
                SlackMessageStatus.PENDING,
                message.getStatus()
        );

        assertEquals(
                0,
                message.getRetryCount()
        );
    }

}