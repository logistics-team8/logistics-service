package com.logistics.notificationservice.infrastructure.persistence;


import com.logistics.notificationservice.domain.slack.SlackMessage;
import com.logistics.notificationservice.domain.slack.SlackMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SlackMessageRepositoryImpl implements SlackMessageRepository {

    private final SlackMessageJpaRepository jpaRepository;

    @Override
    public SlackMessage save(SlackMessage slackMessage) {
        return jpaRepository.save(slackMessage);
    }




}
