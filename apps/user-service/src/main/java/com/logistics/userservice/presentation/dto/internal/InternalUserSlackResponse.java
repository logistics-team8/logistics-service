package com.logistics.userservice.presentation.dto.internal;

import com.logistics.userservice.application.dto.UserSlackInfo;

public record InternalUserSlackResponse(String slackId) {
    public static InternalUserSlackResponse from(UserSlackInfo userSlackInfo) {
        return new InternalUserSlackResponse(userSlackInfo.slackId());
    }
}
