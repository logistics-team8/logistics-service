package com.logistics.hubservice.infrastructure.persistence.hubroute;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.common.security.principal.CustomUserDetails;
import com.logistics.hubservice.PostgreSqlIntegrationTest;
import com.logistics.hubservice.domain.hub.Hub;
import com.logistics.hubservice.domain.hub.HubRepository;
import com.logistics.hubservice.domain.hubroute.HubRoute;
import com.logistics.hubservice.domain.hubroute.HubRouteRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class HubRouteJpaRepositoryAdapterTest extends PostgreSqlIntegrationTest {

    private static final UUID MASTER_ID =
            UUID.fromString("e81cce60-2e94-41cd-9b89-dbf7dfc5f9b5");

    @Autowired
    private HubRepository hubRepository;

    @Autowired
    private HubRouteRepository hubRouteRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearTables() {
        jdbcTemplate.update("delete from p_hub_routes");
        jdbcTemplate.update("delete from p_hubs");
        authenticate(MASTER_ID);
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("경로를 저장하면 감사 필드를 기록하고 활성 방향성 중복을 조회한다")
    void savePersistsRouteAndFindsTheActiveDirectionalDuplicate() {
        Hub sourceHub = saveHub("서울 허브");
        Hub destinationHub = saveHub("대전 허브");

        HubRoute savedRoute = hubRouteRepository.save(HubRoute.create(
                sourceHub.getId(),
                destinationHub.getId(),
                123_400L,
                7_200L
        ));

        assertThat(savedRoute.getId()).isNotNull();
        assertThat(savedRoute.getCreatedAt()).isNotNull();
        assertThat(savedRoute.getUpdatedAt()).isNotNull();
        assertThat(savedRoute.getCreatedBy()).isEqualTo(MASTER_ID);
        assertThat(savedRoute.getUpdatedBy()).isEqualTo(MASTER_ID);
        assertThat(hubRouteRepository.existsBySourceHubIdAndDestinationHubIdAndDeletedAtIsNull(
                sourceHub.getId(), destinationHub.getId()))
                .isTrue();
        assertThat(hubRouteRepository.existsBySourceHubIdAndDestinationHubIdAndDeletedAtIsNull(
                destinationHub.getId(), sourceHub.getId()))
                .isFalse();
    }

    private Hub saveHub(String name) {
        return hubRepository.save(Hub.create(
                name,
                "주소",
                new BigDecimal("37.5000000"),
                new BigDecimal("127.0000000")
        ));
    }

    private void authenticate(UUID userId) {
        CustomUserDetails principal = CustomUserDetails.from(userId, null, null, "MASTER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }
}
