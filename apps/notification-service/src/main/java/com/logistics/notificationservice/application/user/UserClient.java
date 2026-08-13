package com.logistics.notificationservice.application.user;

import java.util.UUID;

public interface UserClient {
    String getSlackId(UUID slackId);
}
