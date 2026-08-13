package com.logistics.notificationservice.infrastructure.persistence;


import com.logistics.notificationservice.application.slack.SlackMessageSearchCondition;
import com.logistics.notificationservice.domain.slack.SlackMessage;
import com.logistics.notificationservice.domain.slack.SlackMessageRepository;
import com.logistics.notificationservice.domain.slack.SlackMessageStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SlackMessageRepositoryImpl implements SlackMessageRepository {

    private final SlackMessageJpaRepository jpaRepository;

    @Override
    public SlackMessage save(SlackMessage slackMessage) {
        return jpaRepository.save(slackMessage);
    }

    @Override
    public List<SlackMessage> findRetryTargets() {

        return jpaRepository
                .findByStatusAndRetryCountLessThan(SlackMessageStatus.PENDING, 3);
    }

    @Override
    public Optional<SlackMessage> findById(UUID slackMessageId) {
        return jpaRepository.findById(slackMessageId);
    }
    @Override
    public Page<SlackMessage> search(SlackMessageSearchCondition condition, Pageable pageable) {

        return jpaRepository.findAll(SlackMessageSpecification.search(condition), pageable);
    }

}
