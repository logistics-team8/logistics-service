package com.logistics.notificationservice.infrastructure.slack.dto;


public record SlackApiRequestDto(
        String slackUserId,
        String channel,
        String text
){
    public static SlackApiRequestDto of(
            String slackUserId,
            String channel,
            String text
    ){
        return new SlackApiRequestDto(slackUserId,channel,text);
    }


}
