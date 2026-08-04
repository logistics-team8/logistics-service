package com.logistics.userservice.application.dto;

import java.util.UUID;

public record UserSlackInfo(UUID userId, String slackId) {}
