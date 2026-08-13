package com.logistics.notificationservice.infrastructure.slack.dto;


public record SlackApiRequestDto(
        String channel,
        String text
){
    public static SlackApiRequestDto of(
            String channel,
            String text
    ){
        return new SlackApiRequestDto(channel,text);
    }


}
