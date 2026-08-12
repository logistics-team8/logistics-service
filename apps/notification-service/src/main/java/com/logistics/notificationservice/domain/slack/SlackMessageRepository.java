package com.logistics.notificationservice.domain.slack;

import com.logistics.notificationservice.application.slack.SlackMessageSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SlackMessageRepository {

    SlackMessage save(SlackMessage slackMessage);

    List<SlackMessage> findRetryTargets();

    Optional<SlackMessage> findById(UUID slackMessageId);

    Page<SlackMessage> search(
            SlackMessageSearchCondition condition,
            Pageable pageable
    );
}
