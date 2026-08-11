package com.logistics.userservice.presentation.dto.auth;

public record TokenResponse(String accessToken) {
    public static TokenResponse from(String accessToken) {
        return new TokenResponse(accessToken);
    }
}
