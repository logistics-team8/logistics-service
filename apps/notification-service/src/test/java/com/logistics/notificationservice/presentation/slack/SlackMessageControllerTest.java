package com.logistics.notificationservice.presentation.slack;

import com.logistics.notificationservice.application.slack.SlackMessageSearchCondition;
import com.logistics.notificationservice.application.slack.SlackMessageService;
import com.logistics.notificationservice.presentation.slack.dto.SlackMessageResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.mock;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SlackMessageController.class)
class SlackMessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SlackMessageService slackMessageService;


    @Test
    void MASTER는_Slack_메시지를_단건_조회할_수_있다() throws Exception {

        // given
        UUID slackMessageId = UUID.randomUUID();

        SlackMessageResponseDto response =
                mock(SlackMessageResponseDto.class);

        when(slackMessageService.getSlackMessage(slackMessageId))
                .thenReturn(response);

        // when & then
        mockMvc.perform(
                        get("/api/slack-messages/{slackMessageId}", slackMessageId)
                                .with(
                                        user("master")
                                                .roles("MASTER")
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void MASTER는_Slack_메시지_목록을_조회할_수_있다() throws Exception {

        // given
        Page<SlackMessageResponseDto> page =
                new PageImpl<>(
                        List.of(
                                mock(SlackMessageResponseDto.class)
                        )
                );

        when(
                slackMessageService.getSlackMessages(
                        any(SlackMessageSearchCondition.class),
                        any()
                )
        ).thenReturn(page);

        // when & then
        mockMvc.perform(
                        get("/api/slack-messages")
                                .param("page", "0")
                                .param("size", "10")
                                .with(
                                        user("master")
                                                .roles("MASTER")
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void Slack_메시지_검색조건이_정상적으로_바인딩된다() throws Exception {

        // given
        Page<SlackMessageResponseDto> page =
                Page.empty();

        when(
                slackMessageService.getSlackMessages(
                        any(SlackMessageSearchCondition.class),
                        any()
                )
        ).thenReturn(page);

        // when
        mockMvc.perform(
                        get("/api/slack-messages")
                                .param("status", "PENDING")
                                .param("recipientSlackId", "U123456789")
                                .param("keyword", "배송")
                                .with(
                                        user("master")
                                                .roles("MASTER")
                                )
                )
                .andExpect(status().isOk());
    }


}