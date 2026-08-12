package com.logistics.deliveryservice.infrastructure.client.user;

import com.logistics.deliveryservice.domain.port.UserSlackProvider;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserSlackClientAdapter implements UserSlackProvider {

    private final UserSlackFeignClient userSlackFeignClient;

    @Override
    public String getSlackId(UUID userId) {
        return userSlackFeignClient.getUserSlackId(userId).getData().slackId();
    }
}
