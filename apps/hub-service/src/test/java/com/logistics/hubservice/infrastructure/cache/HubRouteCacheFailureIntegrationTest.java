package com.logistics.hubservice.infrastructure.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.common.security.principal.CustomUserDetails;
import com.logistics.hubservice.PostgreSqlIntegrationTest;
import com.logistics.hubservice.application.hubroute.command.HubRouteCommandService;
import com.logistics.hubservice.application.hubroute.command.UpdateHubRouteCommand;
import com.logistics.hubservice.application.hubroute.dto.HubRouteResponse;
import com.logistics.hubservice.application.hubroute.query.HubRouteQueryService;
import com.logistics.hubservice.domain.hub.Hub;
import com.logistics.hubservice.domain.hub.HubRepository;
import com.logistics.hubservice.domain.hubroute.HubRoute;
import com.logistics.hubservice.domain.hubroute.HubRouteRepository;
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
@Import(HubRouteCacheFailureIntegrationTest.FailingCacheManagerConfiguration.class)
class HubRouteCacheFailureIntegrationTest extends PostgreSqlIntegrationTest {

    private static final UUID MASTER_ID =
            UUID.fromString("e81cce60-2e94-41cd-9b89-dbf7dfc5f9b5");

    @Autowired
    private HubRepository hubRepository;

    @Autowired
    private HubRouteRepository hubRouteRepository;

    @Autowired
    private HubRouteQueryService hubRouteQueryService;

    @Autowired
    private HubRouteCommandService hubRouteCommandService;

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
    @DisplayName("캐시 조회에 실패하면 DB에서 허브 경로를 조회한다")
    void getOneFallsBackToDatabaseWhenCacheGetFails() {
        Hub sourceHub = saveHub("서울 허브");
        Hub destinationHub = saveHub("대전 허브");
        HubRoute route = hubRouteRepository.save(HubRoute.create(
                sourceHub.getId(),
                destinationHub.getId(),
                123_400L,
                7_200L
        ));

        HubRouteResponse response = hubRouteQueryService.getOne(route.getId());

        assertThat(response.hubRouteId()).isEqualTo(route.getId());
        assertThat(response.distanceMeters()).isEqualTo(123_400L);
    }

    @Test
    @DisplayName("캐시 제거에 실패해도 허브 경로 수정은 완료한다")
    void updatePersistsRouteWhenCacheEvictionFails() {
        Hub sourceHub = saveHub("서울 허브");
        Hub destinationHub = saveHub("대전 허브");
        HubRoute route = hubRouteRepository.save(HubRoute.create(
                sourceHub.getId(),
                destinationHub.getId(),
                123_400L,
                7_200L
        ));

        HubRouteResponse response = hubRouteCommandService.update(
                route.getId(),
                new UpdateHubRouteCommand(130_000L, null));

        assertThat(response.distanceMeters()).isEqualTo(130_000L);
        assertThat(jdbcTemplate.queryForObject(
                "select distance_meters from p_hub_routes where id = ?",
                Long.class,
                route.getId()))
                .isEqualTo(130_000L);
    }

    private Hub saveHub(String name) {
        return hubRepository.save(Hub.create(
                name,
                "주소",
                new BigDecimal("37.5000000"),
                new BigDecimal("127.0000000")
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
            cacheManager.setCaches(List.of(new ConcurrentMapCache("hubRouteById") {
                @Override
                protected Object lookup(Object key) {
                    throw new IllegalStateException("Redis unavailable");
                }

                @Override
                public void evict(Object key) {
                    throw new IllegalStateException("Redis unavailable");
                }
            }, new ConcurrentMapCache("hubRoutePath") {
                @Override
                public void clear() {
                    throw new IllegalStateException("Redis unavailable");
                }
            }));
            return cacheManager;
        }
    }
}
