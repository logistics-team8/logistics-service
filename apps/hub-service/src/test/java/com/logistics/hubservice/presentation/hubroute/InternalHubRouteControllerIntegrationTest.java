package com.logistics.hubservice.presentation.hubroute;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("HubRoute 내부 최단 경로 API 통합 테스트")
class InternalHubRouteControllerIntegrationTest extends PostgreSqlIntegrationTest {

    private static final UUID MASTER_ID =
            UUID.fromString("e81cce60-2e94-41cd-9b89-dbf7dfc5f9b5");

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private FilterChainProxy springSecurityFilterChain;

    @Autowired
    private HubRepository hubRepository;

    @Autowired
    private HubRouteRepository hubRouteRepository;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(springSecurityFilterChain)
                .build();
        jdbcTemplate.update("delete from p_hub_routes");
        jdbcTemplate.update("delete from p_hubs");
        cacheManager.getCache("hubRouteById").clear();
        cacheManager.getCache("hubRoutePath").clear();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("인증 없이 소요시간이 가장 짧은 전체 경로를 조회한다")
    void getsShortestPathWithoutAuthentication() throws Exception {
        Hub sourceHub = saveHub("서울 허브");
        Hub intermediateHub = saveHub("대전 허브");
        Hub destinationHub = saveHub("부산 허브");
        HubRoute firstRoute = saveRoute(sourceHub.getId(), intermediateHub.getId(), 40L, 50L);
        HubRoute secondRoute = saveRoute(intermediateHub.getId(), destinationHub.getId(), 60L, 70L);
        saveRoute(sourceHub.getId(), destinationHub.getId(), 80L, 200L);

        mockMvc.perform(shortestPathRequest(sourceHub.getId(), destinationHub.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sourceHubId").value(sourceHub.getId().toString()))
                .andExpect(jsonPath("$.data.destinationHubId").value(destinationHub.getId().toString()))
                .andExpect(jsonPath("$.data.totalDistanceMeters").value(100L))
                .andExpect(jsonPath("$.data.totalDurationSeconds").value(120L))
                .andExpect(jsonPath("$.data.segments", hasSize(2)))
                .andExpect(jsonPath("$.data.segments[0].sequence").value(1))
                .andExpect(jsonPath("$.data.segments[0].hubRouteId")
                        .value(firstRoute.getId().toString()))
                .andExpect(jsonPath("$.data.segments[1].sequence").value(2))
                .andExpect(jsonPath("$.data.segments[1].hubRouteId")
                        .value(secondRoute.getId().toString()))
                .andExpect(jsonPath("$.error").value(nullValue()));
    }

    @Test
    @DisplayName("같은 활성 허브를 조회하면 빈 구간과 0 합계를 반환한다")
    void sameHubReturnsEmptyPath() throws Exception {
        Hub hub = saveHub("서울 허브");

        mockMvc.perform(shortestPathRequest(hub.getId(), hub.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalDistanceMeters").value(0L))
                .andExpect(jsonPath("$.data.totalDurationSeconds").value(0L))
                .andExpect(jsonPath("$.data.segments", hasSize(0)));
    }

    @Test
    @DisplayName("출발 허브나 도착 허브가 없으면 404를 반환한다")
    void missingHubReturnsHub001() throws Exception {
        Hub sourceHub = saveHub("서울 허브");

        mockMvc.perform(shortestPathRequest(sourceHub.getId(), UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("HUB_001"));
    }

    @Test
    @DisplayName("삭제된 허브의 경로는 조회할 수 없다")
    void deletedHubReturnsHub001() throws Exception {
        Hub sourceHub = saveHub("서울 허브");
        Hub destinationHub = saveHub("부산 허브");
        authenticate();
        destinationHub.delete(MASTER_ID);
        hubRepository.save(destinationHub);
        SecurityContextHolder.clearContext();

        mockMvc.perform(shortestPathRequest(sourceHub.getId(), destinationHub.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("HUB_001"));
    }

    @Test
    @DisplayName("활성 허브 사이에 연결 경로가 없으면 404를 반환한다")
    void unreachableDestinationReturnsHub005() throws Exception {
        Hub sourceHub = saveHub("서울 허브");
        Hub destinationHub = saveHub("부산 허브");

        mockMvc.perform(shortestPathRequest(sourceHub.getId(), destinationHub.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("HUB_005"));
    }

    @Test
    @DisplayName("필수 허브 ID가 없으면 400을 반환한다")
    void missingRequiredHubIdReturnsBadRequest() throws Exception {
        Hub sourceHub = saveHub("서울 허브");

        mockMvc.perform(get("/internal/hub-routes/shortest-path")
                        .param("sourceHubId", sourceHub.getId().toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("내부 최단 경로 API를 공개 Swagger 문서에서 제외한다")
    void internalApiIsHiddenFromOpenApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/internal/hub-routes/shortest-path']")
                        .doesNotExist());
    }

    private Hub saveHub(String name) {
        authenticate();
        Hub hub = hubRepository.save(Hub.create(
                name,
                "주소",
                new BigDecimal("37.5000000"),
                new BigDecimal("127.0000000")));
        SecurityContextHolder.clearContext();
        return hub;
    }

    private HubRoute saveRoute(
            UUID sourceHubId,
            UUID destinationHubId,
            long distanceMeters,
            long durationSeconds) {
        authenticate();
        HubRoute route = hubRouteRepository.save(HubRoute.create(
                sourceHubId,
                destinationHubId,
                distanceMeters,
                durationSeconds));
        SecurityContextHolder.clearContext();
        return route;
    }

    private MockHttpServletRequestBuilder shortestPathRequest(
            UUID sourceHubId,
            UUID destinationHubId) {
        return get("/internal/hub-routes/shortest-path")
                .param("sourceHubId", sourceHubId.toString())
                .param("destinationHubId", destinationHubId.toString());
    }

    private void authenticate() {
        CustomUserDetails principal = CustomUserDetails.from(MASTER_ID, null, null, "MASTER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()));
    }
}
