package com.logistics.userservice.application.dto.user;

import java.util.UUID;

public record UserUpdateCommand(UUID userId, String name, String slackId) {}
