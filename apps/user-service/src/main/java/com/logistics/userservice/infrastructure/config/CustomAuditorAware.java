package com.logistics.userservice.infrastructure.config;

import com.logistics.common.security.CustomUserDetails;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
public class CustomAuditorAware implements AuditorAware<UUID> { // 1. 타입 매핑을 UUID로 변경

    @Override
    public Optional<UUID> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }

        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();

        return Optional.ofNullable(user.getId());
    }
}
