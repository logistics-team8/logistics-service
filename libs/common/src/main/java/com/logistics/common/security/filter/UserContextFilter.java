package com.logistics.common.security.filter;


import com.logistics.common.security.principal.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.BeanDefinitionDsl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

public class UserContextFilter extends OncePerRequestFilter {
    private Logger logger = LoggerFactory.getLogger(UserContextFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String userIdStr = request.getHeader("X-User-Id");
        String hubIdStr = request.getHeader("X-Hub-Id");
        String companyIdStr = request.getHeader("X-Company-Id");
        String roleStr = request.getHeader("X-Role");

        logger.info("userIdStr {}", userIdStr);

        if (StringUtils.hasText(userIdStr)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            UUID userId = UUID.fromString(userIdStr);
            UUID hubId = StringUtils.hasText(hubIdStr) ? UUID.fromString(hubIdStr) : null;
            UUID companyId = StringUtils.hasText(companyIdStr) ? UUID.fromString(companyIdStr) : null;

            setAuthentication(userId, hubId, companyId, roleStr);
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Security Context Set
     * @param userId (PK)
     * @param hubId (PK)
     * @param companyId (PK)
     * @param roleStr 회원 권한
     */
    private void setAuthentication(UUID userId, UUID hubId, UUID companyId,  String roleStr) {
        var userDetails = CustomUserDetails.from(userId, hubId, companyId, roleStr);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
