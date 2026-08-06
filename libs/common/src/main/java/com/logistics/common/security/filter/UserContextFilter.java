package com.logistics.common.security.filter;


import com.logistics.common.security.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

public class UserContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String userIdHeader = request.getHeader("X-User-Id");
        String role = request.getHeader("X-Role");

        if (userIdHeader != null
                && role != null
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            UUID userId = UUID.fromString(userIdHeader);
            setAuthentication(userId, role);
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Security Context Set
     *
     * @param userId Gateway에서 받은 회원 PK
     * @param roleStr User Role String
     */
    private void setAuthentication(UUID userId, String roleStr) {
        var userDetails = CustomUserDetails.from(userId, roleStr);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
