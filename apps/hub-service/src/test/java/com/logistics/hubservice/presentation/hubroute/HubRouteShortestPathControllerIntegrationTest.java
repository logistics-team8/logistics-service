package com.logistics.hubservice.presentation.hubroute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.logistics.common.security.principal.CustomUserDetails;
import com.logistics.hubservice.PostgreSqlIntegrationTest;
import com.logistics.hubservice.domain.hub.Hub;
import com.logistics.hubservice.domain.hub.HubRepository;
import com.logistics.hubservice.domain.hubroute.HubRoute;
import com.logistics.hubservice.domain.hubroute.HubRouteRepository;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
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
@DisplayName("HubRoute 최단 경로 API 통합 테스트")
class HubRouteShortestPathControllerIntegrationTest extends PostgreSqlIntegrationTest {

    private static final UUID MASTER_ID =
            UUID.fromString("e81cce60-2e94-41cd-9b89-dbf7dfc5f9b5");
    private static final UUID HUB_MANAGER_ID =
            UUID.fromString("5136e949-d047-4f31-8da2-e9654dd80f38");

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
    private StringRedisTemplate redisTemplate;

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
    @DisplayName("소요 시간 합계가 가장 짧은 경로의 상세 구간을 조회한다")
    void authenticatedUserGetsShortestPathSegmentsAndTotals() throws Exception {
        Hub sourceHub = saveHub("서울 허브");
        Hub slowIntermediateHub = saveHub("대전 허브");
        Hub fastIntermediateHub = saveHub("대구 허브");
        Hub destinationHub = saveHub("부산 허브");
        saveRoute(sourceHub.getId(), slowIntermediateHub.getId(), 5L, 100L);
        saveRoute(slowIntermediateHub.getId(), destinationHub.getId(), 5L, 100L);
        saveRoute(sourceHub.getId(), fastIntermediateHub.getId(), 4L, 10L);
        saveRoute(fastIntermediateHub.getId(), destinationHub.getId(), 6L, 10L);
        HubRoute fastestRoute = saveRoute(
                sourceHub.getId(), destinationHub.getId(), 11L, 1L);

        mockMvc.perform(authenticated(shortestPathRequest(sourceHub.getId(), destinationHub.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sourceHubId").value(sourceHub.getId().toString()))
                .andExpect(jsonPath("$.data.destinationHubId").value(destinationHub.getId().toString()))
                .andExpect(jsonPath("$.data.totalDistanceMeters").value(11L))
                .andExpect(jsonPath("$.data.totalDurationSeconds").value(1L))
                .andExpect(jsonPath("$.data.segments", hasSize(1)))
                .andExpect(jsonPath("$.data.segments[0].sequence").value(1))
                .andExpect(jsonPath("$.data.segments[0].hubRouteId").value(fastestRoute.getId().toString()))
                .andExpect(jsonPath("$.error").value(nullValue()));
    }

    @Test
    @DisplayName("같은 활성 허브를 조회하면 빈 구간과 0 합계를 반환한다")
    void sameHubReturnsEmptyPath() throws Exception {
        Hub hub = saveHub("서울 허브");

        mockMvc.perform(authenticated(shortestPathRequest(hub.getId(), hub.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalDistanceMeters").value(0L))
                .andExpect(jsonPath("$.data.totalDurationSeconds").value(0L))
                .andExpect(jsonPath("$.data.segments", hasSize(0)));
    }

    @Test
    @DisplayName("출발 허브나 도착 허브가 없으면 404를 반환한다")
    void missingHubReturnsHub001() throws Exception {
        Hub sourceHub = saveHub("서울 허브");

        mockMvc.perform(authenticated(shortestPathRequest(sourceHub.getId(), UUID.randomUUID())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("HUB_001"));
    }

    @Test
    @DisplayName("활성 허브 사이에 연결 경로가 없으면 404를 반환한다")
    void unreachableDestinationReturnsHub005() throws Exception {
        Hub sourceHub = saveHub("서울 허브");
        Hub destinationHub = saveHub("부산 허브");

        mockMvc.perform(authenticated(shortestPathRequest(sourceHub.getId(), destinationHub.getId())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("HUB_005"));
    }

    @Test
    @DisplayName("필수 허브 ID가 없으면 400을 반환한다")
    void missingRequiredHubIdReturnsBadRequest() throws Exception {
        Hub sourceHub = saveHub("서울 허브");

        mockMvc.perform(authenticated(get("/api/v1/hub-routes/shortest-path")
                        .param("sourceHubId", sourceHub.getId().toString())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 최단 경로를 조회할 수 없다")
    void unauthenticatedUserCannotGetShortestPath() throws Exception {
        mockMvc.perform(shortestPathRequest(UUID.randomUUID(), UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("COMMON_101"));
    }

    @Test
    @DisplayName("최단 경로 결과를 Redis에 JSON으로 1시간 저장하고 재사용한다")
    void shortestPathIsCachedAsJsonForOneHour() throws Exception {
        Hub sourceHub = saveHub("서울 허브");
        Hub destinationHub = saveHub("대전 허브");
        HubRoute route = saveRoute(sourceHub.getId(), destinationHub.getId(), 123_400L, 7_200L);
        MockHttpServletRequestBuilder request = authenticated(
                shortestPathRequest(sourceHub.getId(), destinationHub.getId()));

        mockMvc.perform(request)
                .andExpect(status().isOk());

        String cacheKey = pathCacheKey(sourceHub.getId(), destinationHub.getId());
        Set<String> cacheKeys = redisTemplate.keys("hubRoutePath::*");
        assertThat(cacheKeys).containsExactly(cacheKey);
        assertThat(redisTemplate.opsForValue().get(cacheKey))
                .startsWith("{")
                .contains("\"totalDistanceMeters\":123400")
                .contains("\"hubRouteId\":\"" + route.getId() + "\"");
        assertThat(redisTemplate.getExpire(cacheKey, TimeUnit.SECONDS))
                .isBetween(1L, 3_600L);

        jdbcTemplate.update("delete from p_hub_routes where id = ?", route.getId());

        mockMvc.perform(authenticated(shortestPathRequest(sourceHub.getId(), destinationHub.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.segments[0].hubRouteId").value(route.getId().toString()));
    }

    @Test
    @DisplayName("허브 경로를 생성하면 저장된 모든 최단 경로 결과를 제거한다")
    void createHubRouteEvictsAllShortestPathCaches() throws Exception {
        Hub sourceHub = saveHub("서울 허브");
        Hub destinationHub = saveHub("대전 허브");
        saveRoute(sourceHub.getId(), destinationHub.getId(), 10L, 20L);
        mockMvc.perform(authenticated(shortestPathRequest(sourceHub.getId(), destinationHub.getId())))
                .andExpect(status().isOk());
        assertThat(redisTemplate.keys("hubRoutePath::*")).hasSize(1);

        Hub newSourceHub = saveHub("대구 허브");
        Hub newDestinationHub = saveHub("부산 허브");
        mockMvc.perform(master(post("/api/v1/hub-routes"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(newSourceHub.getId(), newDestinationHub.getId(), 20L, 30L)))
                .andExpect(status().isCreated());

        assertThat(redisTemplate.keys("hubRoutePath::*")).isEmpty();
    }

    @Test
    @DisplayName("Swagger 문서에 허브 최단 경로 조회 API를 포함한다")
    void openApiDocumentsShortestPathEndpoint() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/hub-routes/shortest-path'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/hub-routes/shortest-path'].get.description")
                        .value("인증이 필요합니다. 활성 허브 경로를 소요시간 우선으로 탐색하고 소요시간 합계가 같으면 거리 합계를 비교합니다."))
                .andExpect(jsonPath("$.paths['/api/v1/hub-routes/shortest-path'].get.parameters[*].name")
                        .value(org.hamcrest.Matchers.containsInAnyOrder("sourceHubId", "destinationHubId")))
                .andExpect(jsonPath("$.paths['/api/v1/hub-routes/shortest-path'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/hub-routes/shortest-path'].get.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/hub-routes/shortest-path'].get.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/hub-routes/shortest-path'].get.responses['404']").exists());
    }

    private Hub saveHub(String name) {
        authenticate(MASTER_ID, "MASTER");
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
        authenticate(MASTER_ID, "MASTER");
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
        return get("/api/v1/hub-routes/shortest-path")
                .param("sourceHubId", sourceHubId.toString())
                .param("destinationHubId", destinationHubId.toString());
    }

    private String createRequest(
            UUID sourceHubId,
            UUID destinationHubId,
            long distanceMeters,
            long durationSeconds) {
        return """
                {
                  "sourceHubId": "%s",
                  "destinationHubId": "%s",
                  "distanceMeters": %d,
                  "durationSeconds": %d
                }
                """.formatted(sourceHubId, destinationHubId, distanceMeters, durationSeconds);
    }

    private String pathCacheKey(UUID sourceHubId, UUID destinationHubId) {
        return "hubRoutePath::" + sourceHubId + ":" + destinationHubId;
    }

    private MockHttpServletRequestBuilder master(MockHttpServletRequestBuilder request) {
        return authenticated(request, MASTER_ID, "MASTER");
    }

    private MockHttpServletRequestBuilder authenticated(MockHttpServletRequestBuilder request) {
        return authenticated(request, HUB_MANAGER_ID, "HUB_MANAGER");
    }

    private MockHttpServletRequestBuilder authenticated(
            MockHttpServletRequestBuilder request,
            UUID userId,
            String role) {
        return request
                .header("X-User-Id", userId)
                .header("X-Role", role);
    }

    private void authenticate(UUID userId, String role) {
        CustomUserDetails principal = CustomUserDetails.from(userId, null, null, role);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
