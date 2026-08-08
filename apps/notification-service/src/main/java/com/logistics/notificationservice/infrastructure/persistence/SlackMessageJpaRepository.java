package com.logistics.notificationservice.infrastructure.persistence;

import com.logistics.notificationservice.domain.slack.SlackMessage;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.UUID;

public interface SlackMessageJpaRepository extends JpaRepository<SlackMessage, UUID> {

}
