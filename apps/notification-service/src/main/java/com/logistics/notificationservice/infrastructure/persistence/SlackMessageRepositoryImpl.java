package com.logistics.notificationservice.infrastructure.persistence;


import com.logistics.notificationservice.domain.slack.SlackMessage;
import com.logistics.notificationservice.domain.slack.SlackMessageRepository;
import com.logistics.notificationservice.domain.slack.SlackMessageStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

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
                .findByStatusAndRetryCountLessThan(
                        SlackMessageStatus.PENDING,
                        3
                );
    }


}
