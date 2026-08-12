package com.logistics.notificationservice.infrastructure.persistence;

import com.logistics.notificationservice.application.slack.SlackMessageSearchCondition;
import com.logistics.notificationservice.domain.slack.SlackMessage;
import com.logistics.notificationservice.domain.slack.SlackMessageRepository;
import com.logistics.notificationservice.domain.slack.SlackMessageStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;


import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({
        SlackMessageRepositoryImpl.class,
        SlackMessageRepositoryImplTest.JpaAuditingTestConfig.class
})
class SlackMessageRepositoryImplTest {

    @TestConfiguration
    @EnableJpaAuditing
    static class JpaAuditingTestConfig {
    }

    @Autowired
    private SlackMessageRepository slackMessageRepository;

    @Test
    void PENDING이고_retryCount가_3미만인_메시지만_재시도_대상으로_조회한다() {

        // given
        SlackMessage retryTarget =
                SlackMessage.create(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "U111111111",
                        "재시도 대상"
                );

        SlackMessage sentMessage =
                SlackMessage.create(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "U222222222",
                        "이미 전송 완료"
                );

        sentMessage.markAsSent();

        slackMessageRepository.save(retryTarget);
        slackMessageRepository.save(sentMessage);

        // when
        List<SlackMessage> result =
                slackMessageRepository.findRetryTargets();

        // then
        assertThat(result)
                .extracting(SlackMessage::getStatus)
                .containsOnly(SlackMessageStatus.PENDING);

        assertThat(result)
                .extracting(SlackMessage::getSlackMessageId)
                .contains(retryTarget.getSlackMessageId())
                .doesNotContain(sentMessage.getSlackMessageId());
    }

    @Test
    void SlackMessageId로_단건_조회한다() {

        // given
        SlackMessage slackMessage =
                SlackMessage.create(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "U333333333",
                        "단건 조회 테스트"
                );

        SlackMessage saved =
                slackMessageRepository.save(slackMessage);

        // when
        SlackMessage result =
                slackMessageRepository
                        .findById(saved.getSlackMessageId())
                        .orElseThrow();

        // then
        assertThat(result.getSlackMessageId())
                .isEqualTo(saved.getSlackMessageId());

        assertThat(result.getMessage())
                .isEqualTo("단건 조회 테스트");
    }

    @Test
    void SlackMessage_목록을_페이징_조회한다() {

        // given
        for (int i = 0; i < 5; i++) {

            slackMessageRepository.save(
                    SlackMessage.create(
                            UUID.randomUUID(),
                            UUID.randomUUID(),
                            UUID.randomUUID(),
                            "U" + i,
                            "메시지 " + i
                    )
            );
        }

        SlackMessageSearchCondition condition =
                new SlackMessageSearchCondition(
                        null,
                        null,
                        null,
                        null,
                        null
                );

        Pageable pageable =
                PageRequest.of(0, 2);

        // when
        Page<SlackMessage> result =
                slackMessageRepository.search(
                        condition,
                        pageable
                );

        // then
        assertThat(result.getContent())
                .hasSize(2);

        assertThat(result.getTotalElements())
                .isEqualTo(5);

        assertThat(result.getTotalPages())
                .isEqualTo(3);
    }


    @Test
    void status로_SlackMessage를_검색한다() {

        // given
        SlackMessage pending =
                SlackMessage.create(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "U111111111",
                        "PENDING 메시지"
                );

        SlackMessage sent =
                SlackMessage.create(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "U222222222",
                        "SENT 메시지"
                );

        sent.markAsSent();

        slackMessageRepository.save(pending);
        slackMessageRepository.save(sent);

        SlackMessageSearchCondition condition =
                new SlackMessageSearchCondition(
                        SlackMessageStatus.SUCCESS,
                        null,
                        null,
                        null,
                        null
                );

        Pageable pageable =
                PageRequest.of(0, 20);

        // when
        Page<SlackMessage> result =
                slackMessageRepository.search(
                        condition,
                        pageable
                );

        // then
        assertThat(result.getContent())
                .hasSize(1);

        assertThat(result.getContent().get(0).getStatus())
                .isEqualTo(SlackMessageStatus.SUCCESS);
    }


    @Test
    void orderId로_SlackMessage를_검색한다() {

        // given
        UUID targetOrderId = UUID.randomUUID();

        SlackMessage target =
                SlackMessage.create(
                        targetOrderId,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "U111111111",
                        "대상 메시지"
                );

        SlackMessage other =
                SlackMessage.create(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "U222222222",
                        "다른 메시지"
                );

        slackMessageRepository.save(target);
        slackMessageRepository.save(other);

        SlackMessageSearchCondition condition =
                new SlackMessageSearchCondition(
                        null,
                        targetOrderId,
                        null,
                        null,
                        null
                );

        // when
        Page<SlackMessage> result =
                slackMessageRepository.search(
                        condition,
                        PageRequest.of(0, 20)
                );

        // then
        assertThat(result.getContent())
                .hasSize(1);

        assertThat(result.getContent().get(0).getOrderId())
                .isEqualTo(targetOrderId);
    }



    @Test
    void recipientUserId로_SlackMessage를_검색한다() {

        // given
        UUID targetUserId = UUID.randomUUID();

        SlackMessage target =
                SlackMessage.create(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        targetUserId,
                        "U111111111",
                        "대상 메시지"
                );

        SlackMessage other =
                SlackMessage.create(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "U222222222",
                        "다른 메시지"
                );

        slackMessageRepository.save(target);
        slackMessageRepository.save(other);

        SlackMessageSearchCondition condition =
                new SlackMessageSearchCondition(
                        null,
                        null,
                        targetUserId,
                        null,
                        null
                );

        // when
        Page<SlackMessage> result =
                slackMessageRepository.search(
                        condition,
                        PageRequest.of(0, 20)
                );

        // then
        assertThat(result.getContent())
                .hasSize(1);

        assertThat(result.getContent().get(0).getRecipientUserId())
                .isEqualTo(targetUserId);
    }

    @Test
    void recipientSlackId로_SlackMessage를_검색한다() {

        // given
        SlackMessage target =
                SlackMessage.create(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "U_TARGET",
                        "대상 메시지"
                );

        SlackMessage other =
                SlackMessage.create(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "U_OTHER",
                        "다른 메시지"
                );

        slackMessageRepository.save(target);
        slackMessageRepository.save(other);

        SlackMessageSearchCondition condition =
                new SlackMessageSearchCondition(
                        null,
                        null,
                        null,
                        "U_TARGET",
                        null
                );

        // when
        Page<SlackMessage> result =
                slackMessageRepository.search(
                        condition,
                        PageRequest.of(0, 20)
                );

        // then
        assertThat(result.getContent())
                .hasSize(1);

        assertThat(result.getContent().get(0).getRecipientSlackId())
                .isEqualTo("U_TARGET");
    }

    @Test
    void keyword로_메시지_내용을_검색한다() {

        // given
        SlackMessage target =
                SlackMessage.create(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "U111111111",
                        "부산 허브 배송 알림"
                );

        SlackMessage other =
                SlackMessage.create(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "U222222222",
                        "서울 배송 완료"
                );

        slackMessageRepository.save(target);
        slackMessageRepository.save(other);

        SlackMessageSearchCondition condition =
                new SlackMessageSearchCondition(
                        null,
                        null,
                        null,
                        null,
                        "부산"
                );

        // when
        Page<SlackMessage> result =
                slackMessageRepository.search(
                        condition,
                        PageRequest.of(0, 20)
                );

        // then
        assertThat(result.getContent())
                .hasSize(1);

        assertThat(result.getContent().get(0).getMessage())
                .contains("부산");
    }

    @Test
    void 여러_검색조건을_AND로_검색한다() {

        // given
        UUID orderId = UUID.randomUUID();
        UUID recipientUserId = UUID.randomUUID();

        SlackMessage target =
                SlackMessage.create(
                        orderId,
                        UUID.randomUUID(),
                        recipientUserId,
                        "U_TARGET",
                        "긴급 배송 알림"
                );

        SlackMessage other =
                SlackMessage.create(
                        orderId,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "U_OTHER",
                        "긴급 배송 알림"
                );

        slackMessageRepository.save(target);
        slackMessageRepository.save(other);

        SlackMessageSearchCondition condition =
                new SlackMessageSearchCondition(
                        SlackMessageStatus.PENDING,
                        orderId,
                        recipientUserId,
                        "U_TARGET",
                        "긴급"
                );

        // when
        Page<SlackMessage> result =
                slackMessageRepository.search(
                        condition,
                        PageRequest.of(0, 20)
                );

        // then
        assertThat(result.getContent())
                .hasSize(1);

        SlackMessage resultMessage =
                result.getContent().get(0);

        assertThat(resultMessage.getOrderId())
                .isEqualTo(orderId);

        assertThat(resultMessage.getRecipientUserId())
                .isEqualTo(recipientUserId);

        assertThat(resultMessage.getRecipientSlackId())
                .isEqualTo("U_TARGET");

        assertThat(resultMessage.getMessage())
                .contains("긴급");
    }

    @Test
    void SlackMessage_목록을_페이징한다() {

        // given
        for (int i = 0; i < 5; i++) {
            slackMessageRepository.save(
                    SlackMessage.create(
                            UUID.randomUUID(),
                            UUID.randomUUID(),
                            UUID.randomUUID(),
                            "U" + i,
                            "메시지 " + i
                    )
            );
        }

        SlackMessageSearchCondition condition =
                new SlackMessageSearchCondition(
                        null,
                        null,
                        null,
                        null,
                        null
                );

        Pageable pageable =
                PageRequest.of(0, 2);

        // when
        Page<SlackMessage> result =
                slackMessageRepository.search(
                        condition,
                        pageable
                );

        // then
        assertThat(result.getContent())
                .hasSize(2);

        assertThat(result.getTotalElements())
                .isEqualTo(5);

        assertThat(result.getTotalPages())
                .isEqualTo(3);

        assertThat(result.getNumber())
                .isEqualTo(0);
    }


}