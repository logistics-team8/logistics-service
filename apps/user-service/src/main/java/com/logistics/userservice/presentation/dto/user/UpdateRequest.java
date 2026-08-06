package com.logistics.userservice.presentation.dto.user;

import com.logistics.userservice.application.dto.UserUpdateCommand;
import java.util.UUID;

public record UpdateRequest(String name, String slackId) {
    public UserUpdateCommand toCommand(UUID userId) {
        return new UserUpdateCommand(userId, name, slackId);
    }
}
