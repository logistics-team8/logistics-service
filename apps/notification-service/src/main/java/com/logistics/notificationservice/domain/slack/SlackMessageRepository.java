package com.logistics.notificationservice.domain.slack;

public interface SlackMessageRepository {

    SlackMessage save(SlackMessage slackMessage);
}
