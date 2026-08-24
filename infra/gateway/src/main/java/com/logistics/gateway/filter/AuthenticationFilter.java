package com.logistics.gateway.filter;

import com.logistics.gateway.config.PathProperties;
import com.logistics.gateway.error.BusinessException;
import com.logistics.gateway.error.GatewayErrorCode;
import com.logistics.gateway.redis.RedisSessionValidator;
import com.logistics.gateway.redis.RedisUserRoleCache;
import com.logistics.gateway.response.UserRoleResponse;
import com.logistics.gateway.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
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
    private final RedisUserRoleCache roleCache;
    private final WebClient.Builder webClientBuilder;
    private final RedisSessionValidator sessionValidator;
    private final JwtTokenProvider jwtTokenProvider;
    private final PathProperties pathProperties;

    private static final String X_USER_ID = "X-User-Id";
    private static final String X_HUB_ID = "X-Hub-Id";
    private static final String X_COMPANY_ID = "X-Company-Id";
    private static final String X_ROLE = "X-Role";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = exchange.getRequest().getURI().getPath();
        HttpMethod httpMethod = request.getMethod();

        ServerHttpRequest cleanRequest = removeHeader(request);

        boolean isWhitelisted =
                pathProperties.whitelist().stream()
                        .anyMatch(
                                whitelist ->
                                        httpMethod.name().equalsIgnoreCase(whitelist.method())
                                                && pathMatcher.match(whitelist.pattern(), path));

        // Whitelist 체크
        if (isWhitelisted) {
            return chain.filter(exchange.mutate().request(cleanRequest).build());
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
        String sessionId = claims.get("sessionId", String.class);
        String hubId = claims.get("hubId", String.class);
        String companyId = claims.get("companyId", String.class);

        return validateSession(userId, sessionId)
                .then(verifyUserRole(userId))
                .flatMap(
                        role -> {
                            ServerHttpRequest.Builder requestBuilder = cleanRequest.mutate();

                            addHeader(requestBuilder, X_USER_ID, userId);
                            addHeader(requestBuilder, X_HUB_ID, hubId);
                            addHeader(requestBuilder, X_COMPANY_ID, companyId);
                            addHeader(requestBuilder, X_ROLE, role);

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

    /**
     * 헤더 스푸핑 방지용 메서드
     *
     * @param request
     * @return header가 삭제된 Request 객체
     */
    private ServerHttpRequest removeHeader(ServerHttpRequest request) {
        return request.mutate()
                .headers(
                        httpHeaders -> {
                            httpHeaders.remove(X_USER_ID);
                            httpHeaders.remove(X_HUB_ID);
                            httpHeaders.remove(X_COMPANY_ID);
                            httpHeaders.remove(X_ROLE);
                        })
                .build();
    }

    /**
     * Redis 세션 유효성 검증
     *
     * @param userId 회원 PK
     * @param sessionId 토큰 세션 ID
     */
    private Mono<Void> validateSession(String userId, String sessionId) {
        if (!StringUtils.hasText(userId) || !StringUtils.hasText(sessionId)) {
            return Mono.error(new BusinessException(GatewayErrorCode.TOKEN_INVALID));
        }

        return sessionValidator
                .exists(UUID.fromString(userId), UUID.fromString(sessionId))
                .timeout(Duration.ofMillis(300))
                .onErrorResume(
                        e -> {
                            log.error("[ERROR] Redis 세션 조회 실패 userId = {}", userId, e);
                            return Mono.just(true);
                        })
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(new BusinessException(GatewayErrorCode.UNAUTHORIZED)))
                .then();
    }

    /**
     * TODO : Redis 장애 시 트래픽이 집중될 수 있으므로 timeout과 circuit breaker 고려 우선순위 낮음. 회원 권한 Redis에서 조회, 캐시
     * 미스 시 User Service에서 조회
     *
     * @param userId 회원 PK
     * @return 회원 권한
     */
    public Mono<String> verifyUserRole(String userId) {
        return roleCache
                .findByUserId(userId)
                .timeout(Duration.ofMillis(300))
                // Error 시 Empty로 처리하여 switchIfEmpty 작동
                .onErrorResume(
                        e -> {
                            log.warn("[Cache] Role Cache 조회 실패 userId = {}", userId, e);
                            return Mono.empty();
                        })
                // switchIfEmpty 웹플럭스 스트림 형태 IF문 앞의 메서드가 Empty 상태로 완료될 시 후속 메서드 실행
                // Mono.defer() 생성 시간 지연 - 메서드의 인자로 넘어갈 시 즉시 실행 방지
                .switchIfEmpty(Mono.defer(() -> fetchAndCacheUserRole(userId)));
    }

    /**
     * User Service에서 권한 조회 후 Redis에 캐싱
     *
     * @param userId
     * @return
     */
    private Mono<String> fetchAndCacheUserRole(String userId) {
        return verifyRoleFromUserService(userId)
                // flatMap 비동기 작업 체이닝 -> 앞의 비동기 작업 끝나면 다음 비동기 작업을 이어받음.
                // verifyRoleFromUserService -> roleCache.save 형태
                .flatMap(
                        role ->
                                roleCache
                                        .save(userId, role)
                                        // Redis 저장 실패 시 User Service에서 받아온 Role 반환
                                        .onErrorResume(
                                                e -> {
                                                    log.warn(
                                                            "[CACHE] Role Cache 저장 실패 userId = {}",
                                                            userId,
                                                            e);
                                                    return Mono.just(role);
                                                }));
    }

    /**
     * User service에서 권한 조회
     *
     * @param userId
     * @return
     */
    private Mono<String> verifyRoleFromUserService(String userId) {
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
                                        .build(userId))
                .retrieve()
                .onStatus(
                        status -> status.equals(HttpStatus.NOT_FOUND),
                        response -> {
                            log.warn("[FAIL] 해당 유저를 찾을 수 없음 userId = {}", userId);
                            return Mono.error(
                                    new BusinessException(
                                            GatewayErrorCode.USER_NOT_FOUND)); // 또는 401/403 처리
                        })
                .bodyToMono(UserRoleResponse.class)
                .map(UserRoleResponse::role)
                .onErrorMap(
                        e -> {
                            if (e instanceof BusinessException) {
                                return e;
                            }

                            // User 호출 실패 시
                            log.error(
                                    "[ERROR] User Service 호출 실패 userId = {}, message = {}",
                                    userId,
                                    e.getMessage());
                            return new BusinessException(GatewayErrorCode.INTERNAL_SERVER_ERROR);
                        });
    }
}
