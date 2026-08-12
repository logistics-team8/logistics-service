package com.logistics.hubservice.application.hubroute.initialization;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.hubservice.PostgreSqlIntegrationTest;
import com.logistics.hubservice.domain.hub.HubRepository;
import com.logistics.hubservice.domain.hubroute.HubRouteRepository;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(HubRouteDefaultDataServiceIntegrationTest.TestConfig.class)
class HubRouteDefaultDataServiceIntegrationTest extends PostgreSqlIntegrationTest {

    private static final String SYSTEM_USER_ID = "00000000-0000-0000-0000-000000000001";

    @Autowired
    private HubRouteDefaultDataService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void clearTables() {
        jdbcTemplate.update("delete from p_hub_routes");
        jdbcTemplate.update("delete from p_hubs");
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("기본 데이터를 한 번만 저장하고 생성 후 기존 최단 경로 캐시를 제거한다")
    void persistsDefaultDataOnceWithSystemAuditMetadata() {
        Cache pathCache = cacheManager.getCache("hubRoutePath");
        assertThat(pathCache).isNotNull();
        pathCache.put("stale-path", "stale-value");

        HubRouteDefaultDataResult firstResult = service.initialize();
        assertThat(pathCache.get("stale-path")).isNull();
        HubRouteDefaultDataResult secondResult = service.initialize();

        assertThat(firstResult.createdHubCount()).isEqualTo(17);
        assertThat(firstResult.createdHubRouteCount()).isEqualTo(36);
        assertThat(secondResult.createdHubCount()).isZero();
        assertThat(secondResult.createdHubRouteCount()).isZero();
        assertThat(count("p_hubs")).isEqualTo(17);
        assertThat(count("p_hub_routes")).isEqualTo(36);
        assertThat(countCreatedBySystem("p_hubs")).isEqualTo(17);
        assertThat(countCreatedBySystem("p_hub_routes")).isEqualTo(36);
    }

    private long count(String tableName) {
        return jdbcTemplate.queryForObject("select count(*) from " + tableName, Long.class);
    }

    private long countCreatedBySystem(String tableName) {
        return jdbcTemplate.queryForObject(
                "select count(*) from " + tableName + " where created_by = ?::uuid",
                Long.class,
                SYSTEM_USER_ID);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestConfig {

        @Bean
        @Primary
        CacheManager testCacheManager() {
            return new ConcurrentMapCacheManager("hubRouteById", "hubRoutePath");
        }

        @Bean
        HubLocationProvider testHubLocationProvider() {
            AtomicInteger sequence = new AtomicInteger();
            return address -> {
                int index = sequence.incrementAndGet();
                return new HubCoordinates(
                        new BigDecimal("35").add(BigDecimal.valueOf(index, 2)),
                        new BigDecimal("126").add(BigDecimal.valueOf(index, 2)));
            };
        }

        @Bean
        RouteMetricProvider testRouteMetricProvider() {
            AtomicInteger sequence = new AtomicInteger();
            return (source, destination) -> {
                int index = sequence.incrementAndGet();
                return new RouteMetric(10_000L + index, 1_000L + index);
            };
        }

        @Bean
        HubRouteDefaultDataService testHubRouteDefaultDataService(
                HubRepository hubRepository,
                HubRouteRepository hubRouteRepository,
                HubLocationProvider locationProvider,
                RouteMetricProvider metricProvider) {
            return new HubRouteDefaultDataService(
                    hubRepository,
                    hubRouteRepository,
                    locationProvider,
                    metricProvider);
        }
    }
}
