package com.logistics.notificationservice.domain.slack;

import java.util.List;

public interface SlackMessageRepository {

    SlackMessage save(SlackMessage slackMessage);

    List<SlackMessage> findRetryTargets();
}
