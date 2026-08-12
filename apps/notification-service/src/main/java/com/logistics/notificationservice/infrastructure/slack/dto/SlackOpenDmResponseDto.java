package com.logistics.notificationservice.infrastructure.slack.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SlackOpenDmResponseDto {

    private boolean ok;
    private String error;
    private Channel channel;

    public String getErrorCode() {
        return error;
    }

    @Getter
    @NoArgsConstructor
    public static class Channel {
        private String id;
    }
}