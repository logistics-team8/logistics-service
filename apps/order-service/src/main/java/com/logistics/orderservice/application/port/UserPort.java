package com.logistics.orderservice.application.port;

import java.util.UUID;

public interface UserPort {

    UserInfo getUserInfo(UUID userId);

    record UserInfo(
            UUID userId,
            String username,
            String name,
            String slackId,
            UUID hubId,
            UUID companyId,
            String role
    ){
    }
}
