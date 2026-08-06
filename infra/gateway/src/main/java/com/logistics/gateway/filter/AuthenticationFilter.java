package com.logistics.gateway.filter;

import com.logistics.gateway.infrastructure.config.PathProperties;
import com.logistics.gateway.infrastructure.security.JwtTokenProvider;
import com.logistics.gateway.presentation.error.GatewayErrorCode;
import com.logistics.gateway.presentation.exception.BusinessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationFilter implements GlobalFilter, Ordered {
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final ReactiveStringRedisTemplate redisTemplate;
    private final WebClient.Builder webClientBuilder;
    private final JwtTokenProvider jwtTokenProvider;
    private final PathProperties pathProperties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        boolean isWhitelisted =
                pathProperties.whitelist().stream()
                        .anyMatch(pattern -> pathMatcher.match(pattern, path));


        // Whitelist 체크
        if (isWhitelisted) {
            ServerHttpRequest sanitizedRequest =
                    exchange.getRequest()
                            .mutate()
                            // 헤더 스푸핑 방지
                            .headers(
                                    httpHeaders -> {
                                        httpHeaders.remove("X-User-Id");
                                        httpHeaders.remove("X-Hub-Id");
                                        httpHeaders.remove("X-Company-Id");
                                        httpHeaders.remove("X-Role");
                                    })
                            .build();

            return chain.filter(exchange.mutate().request(sanitizedRequest).build());
        }

        // 액세스 토큰 유무 확인
        String accessToken = jwtTokenProvider.resolveAccessToken(exchange);

        if (accessToken == null) {
            // 액세스 토큰이 없는 경우
            return Mono.error(new BusinessException(GatewayErrorCode.UNAUTHORIZED));
        }

        Claims claims;

        try {
            claims = jwtTokenProvider.getAllClaimsFromToken(accessToken);
        } catch (ExpiredJwtException e) {
            // 만료된 토큰 401
            return Mono.error(new BusinessException(GatewayErrorCode.TOKEN_EXPIRED));
        } catch (JwtException e) {
            // 유효하지 않은 토큰 401
            return Mono.error(new BusinessException(GatewayErrorCode.TOKEN_INVALID));
        }

        String userId = claims.getSubject();
        String hubId = claims.get("hubId", String.class);
        String companyId = claims.get("companyId", String.class);

        return verifyUserRole(userId, path)
                .flatMap(
                        role -> {
                            ServerHttpRequest.Builder requestBuilder =
                                    exchange.getRequest()
                                            .mutate()
                                            .headers(headers -> {
                                                headers.remove("X-User-Id");
                                                headers.remove("X-Hub-Id");
                                                headers.remove("X-Company-Id");
                                                headers.remove("X-Role");
                                            });

                            addHeader(requestBuilder, "X-User-Id", userId);
                            addHeader(requestBuilder, "X-Hub-Id", hubId);
                            addHeader(requestBuilder, "X-Company-Id", companyId);
                            addHeader(requestBuilder, "X-Role", role);

                            ServerWebExchange mutatedExchange =
                                    exchange.mutate().request(requestBuilder.build()).build();

                            return chain.filter(mutatedExchange);
                        });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    /**
     * 클레임 Null 체크 후 헤더에 추가
     *
     * @param builder
     * @param header
     * @param value
     */
    private void addHeader(ServerHttpRequest.Builder builder, String header, String value) {
        if (StringUtils.hasText(value)) {
            builder.header(header, value);
        }
    }

    public Mono<String> verifyUserRole(String userId, String path) {
        String redisKey = "user:role:" + userId;

        return redisTemplate
                .opsForValue()
                .get(redisKey)
                // switchIfEmpty 웹플럭스 스트림 형태 IF문 앞의 데이터가 Empty -> 후속 메서드 실행
                // Mono.defer() 생성 시간 지연 - 메서드의 인자로 넘어갈 시 즉시 실행 방지
                .switchIfEmpty(Mono.defer(() -> fetchAndCacheUserRole(userId, redisKey, path)));
    }

    private Mono<String> fetchAndCacheUserRole(String userId, String redisKey, String path) {
        return verifyRoleFromUserService(userId, path)
                // flatMap 비동기 작업 체이닝 -> 앞의 비동기 작업 끝나면 다음 비동기 작업을 이어받음.
                // verifyRoleFromUserService -> saveToRedis 형태
                .flatMap(role -> saveToRedis(redisKey, role));
    }

    private Mono<String> verifyRoleFromUserService(String userId, String path) {
        log.info("[Cache] Role Miss");
        return webClientBuilder
                .build()
                .get()
                .uri(
                        uriBuilder ->
                                uriBuilder
                                        .scheme("http")
                                        .host("user-service")
                                        .path("/internal/v1/users/{userId}/role")
                                        .queryParam("path", path)
                                        .build(userId))
                .retrieve()
                .bodyToMono(String.class)
                .onErrorMap(e -> new BusinessException(GatewayErrorCode.INTERNAL_SERVER_ERROR));
    }

    private Mono<String> saveToRedis(String redisKey, String role) {
        return redisTemplate
                .opsForValue()
                .set(redisKey, role, Duration.ofMinutes(30))
                .thenReturn(role);
    }
}
