package com.logistics.orderservice.infrastructure.client.user;

import com.logistics.orderservice.application.port.UserPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserClientAdapter implements UserPort {

    private final UserFeignClient userFeignClient;


    @Override
    public UserInfo getUserInfo(UUID userId) {
        UserFeignClient.UserResponse response =
                userFeignClient.getUser(userId);

        return new UserInfo(
                response.userId(),
                response.username(),
                response.name(),
                response.slackId(),
                response.hubId(),
                response.companyId(),
                response.role()
        );
    }
}
