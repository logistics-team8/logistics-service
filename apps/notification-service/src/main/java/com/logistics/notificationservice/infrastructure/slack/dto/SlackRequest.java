package com.logistics.notificationservice.infrastructure.slack.dto;


public record SlackRequest (
        String slackUserId,
        String channel,
        String text
){
    public static SlackRequest of(
            String slackUserId,
            String channel,
            String text
    ){
        return new SlackRequest(slackUserId,channel,text);
    }


}
