package com.logistics.userservice.application.token;

import java.util.Date;
import java.util.UUID;

public interface TokenProvider {
    String generateAccessToken(TokenClaims tokenClaims, UUID sessionId);

    String generateRefreshToken(TokenClaims tokenClaims, UUID sessionId);

    TokenPayload getAllClaimsFromAccessToken(String accessToken);

    TokenPayload getAllClaimsFromRefreshToken(String refreshToken);

    Date getExpirationFromAccessToken(String accessToken);
}
