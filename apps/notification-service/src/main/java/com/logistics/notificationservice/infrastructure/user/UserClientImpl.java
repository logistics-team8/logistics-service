package com.logistics.notificationservice.infrastructure.user;

import com.logistics.notificationservice.application.user.UserClient;
import com.logistics.notificationservice.domain.common.exception.NotificationErrorCode;
import com.logistics.notificationservice.domain.common.exception.NotificationException;
import com.logistics.notificationservice.infrastructure.user.dto.InternalUserSlackResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserClientImpl implements UserClient {


   private final UserFeignClient userFeignClient;

    @Override
    public String getSlackId(UUID userId) {

        InternalUserSlackResponseDto response =
                userFeignClient.getSlackId(userId);
        if (response == null
                || response.slackId() == null
                || response.slackId().isBlank()) {

            throw new NotificationException(NotificationErrorCode.USER_NOT_FOUND);
        }

        return response.slackId();
    }
}