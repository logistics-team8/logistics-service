package com.logistics.hubservice.application.hub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.logistics.common.security.principal.CustomUserDetails;
import com.logistics.hubservice.PostgreSqlIntegrationTest;
import com.logistics.hubservice.application.hub.command.HubCommandService;
import com.logistics.hubservice.domain.hub.Hub;
import com.logistics.hubservice.domain.hub.HubRepository;
import com.logistics.hubservice.domain.hubroute.HubRoute;
import com.logistics.hubservice.domain.hubroute.HubRouteRepository;
import com.logistics.hubservice.infrastructure.persistence.hub.HubJpaRepositoryAdapter;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(HubDeleteTransactionIntegrationTest.FailingHubRepositoryConfiguration.class)
@DisplayName("Hub 삭제 트랜잭션 통합 테스트")
class HubDeleteTransactionIntegrationTest extends PostgreSqlIntegrationTest {

    private static final UUID MASTER_ID =
            UUID.fromString("e81cce60-2e94-41cd-9b89-dbf7dfc5f9b5");

    @Autowired
    private HubRepository hubRepository;

    @Autowired
    private HubRouteRepository hubRouteRepository;

    @Autowired
    private HubCommandService hubCommandService;

    @Autowired
    private FailingHubRepository failingHubRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        failingHubRepository.allowSave();
        jdbcTemplate.update("delete from p_hub_routes");
        jdbcTemplate.update("delete from p_hubs");
        authenticate();
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("허브 삭제를 저장하지 못하면 허브와 연결 경로의 삭제를 모두 롤백한다")
    void deleteRollsBackHubAndConnectedRoutesWhenHubSaveFails() {
        Hub targetHub = saveHub("서울 허브");
        Hub otherHub = saveHub("대전 허브");
        Hub thirdHub = saveHub("부산 허브");
        HubRoute outgoingRoute = saveRoute(targetHub.getId(), otherHub.getId());
        HubRoute incomingRoute = saveRoute(thirdHub.getId(), targetHub.getId());
        failingHubRepository.failDeletedHubSave();

        assertThatThrownBy(() -> hubCommandService.delete(targetHub.getId(), MASTER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("허브 삭제 저장 실패");

        assertDeletionWasRolledBack("p_hubs", targetHub.getId());
        assertDeletionWasRolledBack("p_hub_routes", outgoingRoute.getId());
        assertDeletionWasRolledBack("p_hub_routes", incomingRoute.getId());
    }

    private Hub saveHub(String name) {
        return hubRepository.save(Hub.create(
                name,
                "주소",
                new BigDecimal("37.5000000"),
                new BigDecimal("127.0000000")
        ));
    }

    private HubRoute saveRoute(UUID sourceHubId, UUID destinationHubId) {
        return hubRouteRepository.save(HubRoute.create(
                sourceHubId,
                destinationHubId,
                123_400L,
                7_200L
        ));
    }

    private void assertDeletionWasRolledBack(String tableName, UUID id) {
        assertThat(jdbcTemplate.queryForObject(
                "select deleted_at is null and deleted_by is null from " + tableName + " where id = ?",
                Boolean.class,
                id))
                .isTrue();
    }

    private void authenticate() {
        CustomUserDetails principal = CustomUserDetails.from(MASTER_ID, null, null, "MASTER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FailingHubRepositoryConfiguration {

        @Bean
        @Primary
        FailingHubRepository failingHubRepository(
                HubJpaRepositoryAdapter delegate,
                EntityManager entityManager) {
            return new FailingHubRepository(delegate, entityManager);
        }
    }

    static final class FailingHubRepository implements HubRepository {

        private final HubRepository delegate;
        private final EntityManager entityManager;
        private boolean failDeletedHubSave;

        private FailingHubRepository(HubRepository delegate, EntityManager entityManager) {
            this.delegate = delegate;
            this.entityManager = entityManager;
        }

        @Override
        public Hub save(Hub hub) {
            Hub savedHub = delegate.save(hub);
            if (failDeletedHubSave && hub.getDeletedAt() != null) {
                entityManager.flush();
                throw new IllegalStateException("허브 삭제 저장 실패");
            }
            return savedHub;
        }

        @Override
        public Optional<Hub> findByIdAndDeletedAtIsNull(UUID id) {
            return delegate.findByIdAndDeletedAtIsNull(id);
        }

        @Override
        public Page<Hub> findAllByDeletedAtIsNull(Pageable pageable) {
            return delegate.findAllByDeletedAtIsNull(pageable);
        }

        @Override
        public Page<Hub> search(String keyword, Pageable pageable) {
            return delegate.search(keyword, pageable);
        }

        void failDeletedHubSave() {
            failDeletedHubSave = true;
        }

        void allowSave() {
            failDeletedHubSave = false;
        }
    }
}
