package com.logistics.hubservice.application.hubroute;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.logistics.common.security.principal.CustomUserDetails;
import com.logistics.hubservice.PostgreSqlIntegrationTest;
import com.logistics.hubservice.application.hubroute.command.HubRouteCommandService;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class HubRouteCommandServiceSecurityIntegrationTest extends PostgreSqlIntegrationTest {

    private static final UUID HUB_MANAGER_ID =
            UUID.fromString("5136e949-d047-4f31-8da2-e9654dd80f38");

    @Autowired
    private HubRouteCommandService hubRouteCommandService;

    @BeforeEach
    void authenticateHubManager() {
        CustomUserDetails principal = CustomUserDetails.from(
                HUB_MANAGER_ID,
                null,
                null,
                "HUB_MANAGER"
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("MASTER가 아닌 사용자는 서비스 계층에서도 허브 경로를 삭제할 수 없다")
    void nonMasterCannotDeleteHubRouteThroughCommandService() {
        assertThatThrownBy(() -> hubRouteCommandService.delete(UUID.randomUUID(), HUB_MANAGER_ID))
                .isInstanceOf(AccessDeniedException.class);
    }
}
