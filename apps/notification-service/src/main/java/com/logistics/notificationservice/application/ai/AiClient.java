package com.logistics.notificationservice.application.ai;

public interface AiClient {
    AiResult calculateDispatchDeadline(String prompt);
}
