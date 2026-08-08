package com.logistics.userservice.application.dto;

import java.util.UUID;

public record UserUpdateCommand(UUID userId, String name, String slackId) {}
