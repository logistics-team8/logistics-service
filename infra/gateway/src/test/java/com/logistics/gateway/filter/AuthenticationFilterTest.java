package com.logistics.gateway.filter;

import com.logistics.gateway.config.PathProperties;
import com.logistics.gateway.redis.RedisUserRoleCache;
import com.logistics.gateway.security.JwtTokenProvider;
import com.logistics.gateway.error.GatewayErrorCode;
import com.logistics.gateway.error.BusinessException;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class AuthenticationFilterTest {
    @Mock
    RedisUserRoleCache roleCache;

    @Mock WebClient.Builder webClientBuilder;

    @Mock JwtTokenProvider jwtTokenProvider;

    @Mock PathProperties pathProperties;

    AuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter =
                new AuthenticationFilter(
                        roleCache, webClientBuilder, jwtTokenProvider, pathProperties);
    }

    @Test
    @DisplayName("화이트리스트면 통과")
    void whitelistPass() {
        // given
        ServerWebExchange exchange =
                MockServerWebExchange.from(MockServerHttpRequest.get("/auth/login").build());

        given(pathProperties.whitelist())
                .willReturn(List.of(new PathProperties.PathPattern("GET", "/auth/**")));

        // when & then
        StepVerifier.create(filter.filter(exchange, chain -> Mono.empty()))
                .verifyComplete();
    }

    @Test
    @DisplayName("액세스 토큰이 없으면 예외 발생")
    void accessTokenNotFound() {
        // given
        ServerWebExchange exchange =
                MockServerWebExchange.from(MockServerHttpRequest.get("/api/orders").build());

        given(pathProperties.whitelist()).willReturn(List.of());

        given(jwtTokenProvider.resolveAccessToken(exchange)).willReturn(null);

        // when & then
        StepVerifier.create(filter.filter(exchange, chain -> Mono.empty()))
                .expectError(BusinessException.class)
                .verify();
    }


    @Test
    @DisplayName("만료된 token이면 예외 반환")
    void expiredToken() {
        // given
        ServerWebExchange exchange =
                MockServerWebExchange.from(MockServerHttpRequest.get("/api/orders").build());

        given(pathProperties.whitelist()).willReturn(List.of());

        given(jwtTokenProvider.resolveAccessToken(exchange)).willReturn("expired-token");

        given(jwtTokenProvider.getAllClaimsFromToken("expired-token"))
                .willThrow(mock(ExpiredJwtException.class));

        // when & then
        StepVerifier.create(filter.filter(exchange, chain -> Mono.empty()))
                .expectErrorSatisfies(
                        error -> {
                            BusinessException exception = (BusinessException) error;

                            assert exception.getErrorCode() == GatewayErrorCode.TOKEN_EXPIRED;
                        })
                .verify();
    }

    // TODO : 정상 진행부분 테스트 코드 작성 필요
}
