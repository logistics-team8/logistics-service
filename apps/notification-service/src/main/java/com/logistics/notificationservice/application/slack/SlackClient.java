package com.logistics.notificationservice.application.slack;

public interface SlackClient {

    void sendMessage(
            String slackUserId,
            String message
    );
}
