package com.logistics.gateway.filter;


import com.logistics.gateway.infrastructure.config.PathProperties;
import com.logistics.gateway.infrastructure.security.JwtTokenProvider;
import com.logistics.gateway.presentation.error.GatewayErrorCode;
import com.logistics.gateway.presentation.exception.BusinessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationFilter implements GlobalFilter, Ordered {
    private final JwtTokenProvider jwtTokenProvider;
    private final PathProperties pathProperties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        boolean isWhitelisted = pathProperties.whitelist().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));

        // Whitelist 체크
        if (isWhitelisted) {
            return chain.filter(exchange);
        }

        // 액세스 확인
        String accessToken = jwtTokenProvider.resolveAccessToken(exchange);

        if (accessToken == null) {
            // 액세스 토큰이 없는 경우
            throw new BusinessException(GatewayErrorCode.UNAUTHORIZED);
        }
        try {
            // 액세스 토큰 검증 및 파싱
            Claims claims = jwtTokenProvider.getAllClaimsFromToken(accessToken);
            String userId = claims.getSubject();
            String hubId = claims.get("hubId", String.class);
            String companyId = claims.get("companyId", String.class);
            String role = claims.get("role", String.class);

            // TODO : REDIS Role 요청 캐싱

            ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate();

            addHeader(requestBuilder, "X-User-Id", userId);
            addHeader(requestBuilder, "X-Hub-Id", hubId);
            addHeader(requestBuilder, "X-Company-Id", companyId);
            addHeader(requestBuilder, "X-Role", role);

            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(requestBuilder.build())
                    .build();
            return chain.filter(mutatedExchange);
        } catch (ExpiredJwtException e) {
            // 만료된 토큰 401
            throw new BusinessException(GatewayErrorCode.TOKEN_EXPIRED);
        } catch (SignatureException | MalformedJwtException | IllegalArgumentException e) {
            // 유효하지 않은 토큰 401
            throw new BusinessException(GatewayErrorCode.TOKEN_INVALID);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private void addHeader(ServerHttpRequest.Builder builder, String header, String value) {
        if (StringUtils.hasText(value)) {
            builder.header(header, value);
        }
    }

}