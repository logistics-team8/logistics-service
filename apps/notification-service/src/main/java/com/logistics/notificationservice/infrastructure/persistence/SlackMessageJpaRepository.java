package com.logistics.notificationservice.infrastructure.persistence;

import com.logistics.notificationservice.domain.slack.SlackMessage;
import com.logistics.notificationservice.domain.slack.SlackMessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;
import java.util.UUID;

public interface SlackMessageJpaRepository extends JpaRepository<SlackMessage, UUID> {

    List<SlackMessage> findByStatusAndRetryCountLessThan(
            SlackMessageStatus status,
            Integer retryCount
    );
}
