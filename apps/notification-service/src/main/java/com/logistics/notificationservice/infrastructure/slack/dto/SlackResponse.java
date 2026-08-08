package com.logistics.notificationservice.infrastructure.slack.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class SlackResponse {

    private boolean ok;

    private String channel;

    private String ts;
    @JsonProperty("error")
    private String errorCode;

}
