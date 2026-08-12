package com.logistics.notificationservice.presentation.slack;

import com.logistics.common.response.ApiResponse;
import com.logistics.notificationservice.application.slack.SlackMessageSearchCondition;
import com.logistics.notificationservice.application.slack.SlackMessageService;
import com.logistics.notificationservice.presentation.slack.dto.SlackMessageResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
@Tag(name = "Slack Message", description = "Slack 메시지 관리 API")
@RestController
@RequestMapping("/api/slack-messages")
@RequiredArgsConstructor
public class SlackMessageController {

    private final SlackMessageService slackMessageService;


    @Operation(summary = "Slack 메시지 단건 조회", description = "MASTER 관리자가 Slack 메시지 상세 정보를 조회합니다.")
    @GetMapping("/{slackMessageId}")
    @PreAuthorize("hasRole('MASTER')")
    public ResponseEntity<ApiResponse<SlackMessageResponseDto>>
    getSlackMessage(@PathVariable UUID slackMessageId){
        SlackMessageResponseDto response = slackMessageService.getSlackMessage(slackMessageId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }


    @Operation(summary = "Slack 메시지 목록 조회", description = "MASTER 관리자가 Slack 메시지 발송 이력을 검색 및 페이징 조회합니다.")
    @GetMapping
    @PreAuthorize("hasRole('MASTER')")
    public ResponseEntity<ApiResponse<Page<SlackMessageResponseDto>>>
    getSlackMessages(SlackMessageSearchCondition condition,
                     @ParameterObject
                     @PageableDefault(
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {

        Page<SlackMessageResponseDto> response = slackMessageService.getSlackMessages(condition, pageable);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
