package com.logistics.notificationservice.infrastructure.security;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpHeaders;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ServiceTokenFilter
        extends OncePerRequestFilter {

    private static final String DELIVERY_SERVICE = "delivery-service";


    private final ServiceTokenProvider serviceTokenProvider;

    @Override
    protected boolean shouldNotFilter(
            HttpServletRequest request
    ) {

        return !request.getRequestURI()
                .startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);


        if (authorization == null || !authorization.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String token = authorization.substring(7);
        try {
            Claims claims = serviceTokenProvider.parse(token);

            String type = claims.get("type", String.class);

            String serviceName = claims.getSubject();

            // 삭제
            System.out.println("type = " + type);
            System.out.println("serviceName = " + serviceName);



            // 서비스 토큰인지 검사
            if (!"SERVICE".equals(type)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            // 주문 알림 API는 delivery-service만 허용
            if (request.getRequestURI()
                    .startsWith("/internal/v1/notifications/orders") && !DELIVERY_SERVICE.equals(serviceName)) {

                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;

            }

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            serviceName, null, List.of(new SimpleGrantedAuthority("ROLE_SERVICE")));

            SecurityContextHolder
                    .getContext()
                    .setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } catch (JwtException | IllegalArgumentException e) {

            response.setStatus(
                    HttpServletResponse.SC_UNAUTHORIZED
            );
        }
    }
}