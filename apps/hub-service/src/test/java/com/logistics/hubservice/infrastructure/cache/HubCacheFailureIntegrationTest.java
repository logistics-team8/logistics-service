package com.logistics.hubservice.infrastructure.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.common.security.principal.CustomUserDetails;
import com.logistics.hubservice.PostgreSqlIntegrationTest;
import com.logistics.hubservice.application.hub.command.HubCommandService;
import com.logistics.hubservice.application.hub.command.UpdateHubCommand;
import com.logistics.hubservice.application.hub.dto.HubResponse;
import com.logistics.hubservice.application.hub.query.HubQueryService;
import com.logistics.hubservice.domain.hub.Hub;
import com.logistics.hubservice.domain.hub.HubRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(HubCacheFailureIntegrationTest.FailingCacheManagerConfiguration.class)
@DisplayName("Hub 캐시 장애 통합 테스트")
class HubCacheFailureIntegrationTest extends PostgreSqlIntegrationTest {

    private static final UUID MASTER_ID =
            UUID.fromString("e81cce60-2e94-41cd-9b89-dbf7dfc5f9b5");

    @Autowired
    private HubRepository hubRepository;

    @Autowired
    private HubQueryService hubQueryService;

    @Autowired
    private HubCommandService hubCommandService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from p_hub_routes");
        jdbcTemplate.update("delete from p_hubs");
        authenticate();
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("캐시 조회에 실패하면 DB에서 허브를 조회한다")
    void getOneFallsBackToDatabaseWhenCacheGetFails() {
        Hub hub = saveHub("서울 허브");

        HubResponse response = hubQueryService.getOne(hub.getId());

        assertThat(response.hubId()).isEqualTo(hub.getId());
        assertThat(response.name()).isEqualTo("서울 허브");
    }

    @Test
    @DisplayName("캐시 제거에 실패해도 허브 수정은 완료한다")
    void updatePersistsHubWhenCacheEvictionFails() {
        Hub hub = saveHub("서울 허브");

        HubResponse response = hubCommandService.update(
                hub.getId(),
                new UpdateHubCommand("동서울 허브", null, null, null));

        assertThat(response.name()).isEqualTo("동서울 허브");
        assertThat(jdbcTemplate.queryForObject(
                "select name from p_hubs where id = ?",
                String.class,
                hub.getId()))
                .isEqualTo("동서울 허브");
    }

    @Test
    @DisplayName("캐시 제거에 실패해도 허브 삭제는 완료한다")
    void deletePersistsHubWhenCacheEvictionFails() {
        Hub hub = saveHub("서울 허브");

        hubCommandService.delete(hub.getId(), MASTER_ID);

        assertThat(jdbcTemplate.queryForObject(
                "select deleted_at is not null from p_hubs where id = ?",
                Boolean.class,
                hub.getId()))
                .isTrue();
        assertThat(jdbcTemplate.queryForObject(
                "select deleted_by from p_hubs where id = ?",
                UUID.class,
                hub.getId()))
                .isEqualTo(MASTER_ID);
    }

    private Hub saveHub(String name) {
        return hubRepository.save(Hub.create(
                name,
                "서울특별시 송파구 송파대로 55",
                new BigDecimal("37.5145751"),
                new BigDecimal("127.1122451")
        ));
    }

    private void authenticate() {
        CustomUserDetails principal = CustomUserDetails.from(MASTER_ID, null, null, "MASTER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FailingCacheManagerConfiguration {

        @Bean
        @Primary
        CacheManager failingCacheManager() {
            SimpleCacheManager cacheManager = new SimpleCacheManager();
            cacheManager.setCaches(List.of(new ConcurrentMapCache("hubById") {
                @Override
                protected Object lookup(Object key) {
                    throw new IllegalStateException("Redis unavailable");
                }

                @Override
                public void evict(Object key) {
                    throw new IllegalStateException("Redis unavailable");
                }
            }));
            return cacheManager;
        }
    }
}
