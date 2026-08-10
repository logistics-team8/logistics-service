package com.logistics.hubservice.presentation.hubroute;

import static org.assertj.core.api.Assertions.assertThat;
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
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
@DisplayName("HubRoute API 통합 테스트")
class HubRouteControllerIntegrationTest extends PostgreSqlIntegrationTest {

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
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("MASTER는 허브 경로를 생성할 수 있다")
    void masterCanCreateHubRouteWithSuccessEnvelope() throws Exception {
        Hub sourceHub = saveHub("서울 허브");
        Hub destinationHub = saveHub("대전 허브");

        mockMvc.perform(master(post("/api/v1/hub-routes"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(sourceHub.getId(), destinationHub.getId(), 123_400L, 7_200L)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.hubRouteId").isNotEmpty())
                .andExpect(jsonPath("$.data.sourceHubId").value(sourceHub.getId().toString()))
                .andExpect(jsonPath("$.data.destinationHubId").value(destinationHub.getId().toString()))
                .andExpect(jsonPath("$.data.distanceMeters").value(123_400L))
                .andExpect(jsonPath("$.data.durationSeconds").value(7_200L))
                .andExpect(jsonPath("$.error").value(nullValue()));
    }

    @Test
    @DisplayName("이동 거리 또는 소요 시간이 유효하지 않으면 400 Bad Request를 반환한다")
    void createRejectsInvalidDistanceAndDuration() throws Exception {
        Hub sourceHub = saveHub("서울 허브");
        Hub destinationHub = saveHub("대전 허브");

        mockMvc.perform(master(post("/api/v1/hub-routes"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(sourceHub.getId(), destinationHub.getId(), 0L, -1L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("COMMON_002"));
    }

    @Test
    @DisplayName("MASTER가 아닌 사용자는 허브 경로를 생성할 수 없다")
    void nonMasterCannotCreateHubRoute() throws Exception {
        Hub sourceHub = saveHub("서울 허브");
        Hub destinationHub = saveHub("대전 허브");

        mockMvc.perform(authenticated(post("/api/v1/hub-routes"), HUB_MANAGER_ID, "HUB_MANAGER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(sourceHub.getId(), destinationHub.getId(), 1L, 1L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("COMMON_102"));
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 허브 경로를 생성할 수 없다")
    void unauthenticatedUserCannotCreateHubRoute() throws Exception {
        mockMvc.perform(post("/api/v1/hub-routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(UUID.randomUUID(), UUID.randomUUID(), 1L, 1L)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("COMMON_101"));
    }

    @Test
    @DisplayName("출발 또는 도착 허브가 존재하지 않으면 경로를 생성할 수 없다")
    void createRejectsMissingHubWithHub001() throws Exception {
        Hub sourceHub = saveHub("서울 허브");

        mockMvc.perform(master(post("/api/v1/hub-routes"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(sourceHub.getId(), UUID.randomUUID(), 1L, 1L)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("HUB_001"));
    }

    @Test
    @DisplayName("출발 허브와 도착 허브가 동일하면 경로를 생성할 수 없다")
    void createRejectsTheSameSourceAndDestinationHubWithHub004() throws Exception {
        Hub hub = saveHub("서울 허브");

        mockMvc.perform(master(post("/api/v1/hub-routes"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(hub.getId(), hub.getId(), 1L, 1L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("HUB_004"));
    }

    @Test
    @DisplayName("동일한 방향의 활성 경로가 이미 있으면 경로를 중복 등록할 수 없다")
    void createRejectsAnActiveDuplicateWithHub003() throws Exception {
        Hub sourceHub = saveHub("서울 허브");
        Hub destinationHub = saveHub("대전 허브");
        String request = createRequest(sourceHub.getId(), destinationHub.getId(), 1L, 1L);

        mockMvc.perform(master(post("/api/v1/hub-routes"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated());

        mockMvc.perform(master(post("/api/v1/hub-routes"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("HUB_003"));
    }

    @Test
    @DisplayName("출발 허브와 도착 허브 조건을 조합하면 활성 경로만 검색한다")
    void searchCombinesHubConditionsAndExcludesDeletedRoutes() throws Exception {
        Hub sourceHub = saveHub("서울 허브");
        Hub destinationHub = saveHub("대전 허브");
        Hub otherHub = saveHub("부산 허브");
        HubRoute matchingRoute = saveRoute(sourceHub.getId(), destinationHub.getId());
        saveRoute(sourceHub.getId(), otherHub.getId());
        saveRoute(otherHub.getId(), destinationHub.getId());
        HubRoute deletedRoute = saveRoute(destinationHub.getId(), sourceHub.getId());
        jdbcTemplate.update(
                "update p_hub_routes set deleted_at = current_timestamp where id = ?",
                deletedRoute.getId());

        mockMvc.perform(authenticated(get("/api/v1/hub-routes")
                        .param("sourceHubId", sourceHub.getId().toString())
                        .param("destinationHubId", destinationHub.getId().toString()),
                HUB_MANAGER_ID,
                "HUB_MANAGER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].hubRouteId").value(matchingRoute.getId().toString()))
                .andExpect(jsonPath("$.error").value(nullValue()));

        mockMvc.perform(authenticated(get("/api/v1/hub-routes"), HUB_MANAGER_ID, "HUB_MANAGER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(3));
    }

    @ParameterizedTest
    @ValueSource(ints = {10, 30, 50})
    @DisplayName("허용한 페이지 크기로 허브 경로 검색 결과를 조회한다")
    void searchSupportsAllowedPageSizes(int pageSize) throws Exception {
        mockMvc.perform(authenticated(get("/api/v1/hub-routes")
                        .param("size", String.valueOf(pageSize)),
                HUB_MANAGER_ID,
                "HUB_MANAGER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(pageSize));
    }

    @Test
    @DisplayName("페이지 크기를 지정하지 않거나 지원하지 않는 크기를 지정하면 10개로 조회한다")
    void searchUsesPageSizeTenAsDefaultAndFallback() throws Exception {
        mockMvc.perform(authenticated(get("/api/v1/hub-routes"), HUB_MANAGER_ID, "HUB_MANAGER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(10));

        mockMvc.perform(authenticated(get("/api/v1/hub-routes")
                        .param("size", "25"),
                HUB_MANAGER_ID,
                "HUB_MANAGER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(10));
    }

    @Test
    @DisplayName("생성일과 수정일을 지정한 방향으로 허브 경로를 정렬한다")
    void searchSupportsCreatedAtAndUpdatedAtInBothDirections() throws Exception {
        Hub sourceHub = saveHub("서울 허브");
        Hub destinationHub = saveHub("대전 허브");
        Hub otherSourceHub = saveHub("부산 허브");
        Hub otherDestinationHub = saveHub("광주 허브");
        HubRoute olderRoute = saveRoute(sourceHub.getId(), destinationHub.getId());
        HubRoute newerRoute = saveRoute(otherSourceHub.getId(), otherDestinationHub.getId());
        updateRouteTimestamps(
                olderRoute,
                LocalDateTime.of(2026, 8, 8, 9, 0),
                LocalDateTime.of(2026, 8, 8, 11, 0));
        updateRouteTimestamps(
                newerRoute,
                LocalDateTime.of(2026, 8, 8, 10, 0),
                LocalDateTime.of(2026, 8, 8, 10, 30));

        mockMvc.perform(authenticated(get("/api/v1/hub-routes")
                        .param("sort", "createdAt,asc"),
                HUB_MANAGER_ID,
                "HUB_MANAGER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].hubRouteId").value(olderRoute.getId().toString()));

        mockMvc.perform(authenticated(get("/api/v1/hub-routes")
                        .param("sort", "updatedAt,desc"),
                HUB_MANAGER_ID,
                "HUB_MANAGER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].hubRouteId").value(olderRoute.getId().toString()));
    }

    @Test
    @DisplayName("허용하지 않는 정렬 필드로 허브 경로를 검색할 수 없다")
    void searchRejectsUnsupportedSortProperty() throws Exception {
        mockMvc.perform(authenticated(get("/api/v1/hub-routes")
                        .param("sort", "sourceHubId,asc"),
                HUB_MANAGER_ID,
                "HUB_MANAGER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("COMMON_001"));
    }

    @Test
    @DisplayName("허브 경로 검색 결과는 Redis에 저장하지 않는다")
    void searchDoesNotCachePageResults() throws Exception {
        Hub sourceHub = saveHub("서울 허브");
        Hub destinationHub = saveHub("대전 허브");
        saveRoute(sourceHub.getId(), destinationHub.getId());

        mockMvc.perform(authenticated(get("/api/v1/hub-routes"), HUB_MANAGER_ID, "HUB_MANAGER"))
                .andExpect(status().isOk());

        assertThat(redisTemplate.keys("hubRoute*")).isEmpty();
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 허브 경로를 검색할 수 없다")
    void unauthenticatedUserCannotSearchHubRoutes() throws Exception {
        mockMvc.perform(get("/api/v1/hub-routes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("COMMON_101"));
    }

    @Test
    @DisplayName("인증된 사용자는 활성 허브 경로를 단건 조회할 수 있다")
    void authenticatedUserCanGetOneActiveHubRoute() throws Exception {
        Hub sourceHub = saveHub("서울 허브");
        Hub destinationHub = saveHub("대전 허브");
        HubRoute route = saveRoute(sourceHub.getId(), destinationHub.getId());

        mockMvc.perform(authenticated(
                        get("/api/v1/hub-routes/{hubRouteId}", route.getId()),
                        HUB_MANAGER_ID,
                        "HUB_MANAGER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hubRouteId").value(route.getId().toString()))
                .andExpect(jsonPath("$.data.sourceHubId").value(sourceHub.getId().toString()))
                .andExpect(jsonPath("$.data.destinationHubId").value(destinationHub.getId().toString()))
                .andExpect(jsonPath("$.data.distanceMeters").value(123_400L))
                .andExpect(jsonPath("$.data.durationSeconds").value(7_200L))
                .andExpect(jsonPath("$.error").value(nullValue()));
    }

    @Test
    @DisplayName("존재하지 않는 허브 경로를 조회하면 404를 반환한다")
    void getOneRejectsMissingRouteWithHub002() throws Exception {
        mockMvc.perform(authenticated(
                        get("/api/v1/hub-routes/{hubRouteId}", UUID.randomUUID()),
                        HUB_MANAGER_ID,
                        "HUB_MANAGER"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("HUB_002"));
    }

    @Test
    @DisplayName("논리 삭제된 허브 경로를 조회하면 404를 반환한다")
    void getOneRejectsDeletedRouteWithHub002() throws Exception {
        Hub sourceHub = saveHub("서울 허브");
        Hub destinationHub = saveHub("대전 허브");
        HubRoute route = saveRoute(sourceHub.getId(), destinationHub.getId());
        jdbcTemplate.update(
                "update p_hub_routes set deleted_at = current_timestamp where id = ?",
                route.getId());

        mockMvc.perform(authenticated(
                        get("/api/v1/hub-routes/{hubRouteId}", route.getId()),
                        HUB_MANAGER_ID,
                        "HUB_MANAGER"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("HUB_002"));
    }

    @Test
    @DisplayName("단건 조회 결과를 Redis에 JSON으로 1시간 캐싱한다")
    void getOneCachesJsonResponseForOneHour() throws Exception {
        Hub sourceHub = saveHub("서울 허브");
        Hub destinationHub = saveHub("대전 허브");
        HubRoute route = saveRoute(sourceHub.getId(), destinationHub.getId());
        MockHttpServletRequestBuilder request = authenticated(
                get("/api/v1/hub-routes/{hubRouteId}", route.getId()),
                HUB_MANAGER_ID,
                "HUB_MANAGER");

        mockMvc.perform(request)
                .andExpect(status().isOk());

        Set<String> cacheKeys = redisTemplate.keys("hubRouteById::*");
        assertThat(cacheKeys).hasSize(1);
        String cacheKey = cacheKeys.iterator().next();
        assertThat(redisTemplate.opsForValue().get(cacheKey))
                .startsWith("{")
                .contains("\"hubRouteId\"");
        assertThat(redisTemplate.getExpire(cacheKey, TimeUnit.SECONDS))
                .isBetween(1L, 3_600L);

        jdbcTemplate.update("delete from p_hub_routes where id = ?", route.getId());

        mockMvc.perform(authenticated(
                        get("/api/v1/hub-routes/{hubRouteId}", route.getId()),
                        HUB_MANAGER_ID,
                        "HUB_MANAGER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hubRouteId").value(route.getId().toString()));
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 허브 경로를 단건 조회할 수 없다")
    void unauthenticatedUserCannotGetOneHubRoute() throws Exception {
        mockMvc.perform(get("/api/v1/hub-routes/{hubRouteId}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("COMMON_101"));
    }

    @Test
    @DisplayName("Swagger 문서에 허브 경로 생성과 조회 API를 포함한다")
    void openApiDocumentsTheHubRouteCreateEndpoint() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/hub-routes'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/hub-routes'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/hub-routes'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/hub-routes'].post.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/hub-routes'].post.responses['403']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/hub-routes'].post.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/hub-routes'].post.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/hub-routes'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/hub-routes'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/hub-routes'].get.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/hub-routes'].get.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/hub-routes/{hubRouteId}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/hub-routes/{hubRouteId}'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/hub-routes/{hubRouteId}'].get.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/hub-routes/{hubRouteId}'].get.responses['404']").exists());
    }

    private Hub saveHub(String name) {
        authenticate(MASTER_ID, "MASTER");
        Hub hub = hubRepository.save(Hub.create(
                name,
                "주소",
                new BigDecimal("37.5000000"),
                new BigDecimal("127.0000000")
        ));
        SecurityContextHolder.clearContext();
        return hub;
    }

    private HubRoute saveRoute(UUID sourceHubId, UUID destinationHubId) {
        authenticate(MASTER_ID, "MASTER");
        HubRoute route = hubRouteRepository.save(HubRoute.create(
                sourceHubId,
                destinationHubId,
                123_400L,
                7_200L
        ));
        SecurityContextHolder.clearContext();
        return route;
    }

    private void updateRouteTimestamps(
            HubRoute route, LocalDateTime createdAt, LocalDateTime updatedAt) {
        jdbcTemplate.update(
                "update p_hub_routes set created_at = ?, updated_at = ? where id = ?",
                createdAt,
                updatedAt,
                route.getId());
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

    private MockHttpServletRequestBuilder master(MockHttpServletRequestBuilder request) {
        return authenticated(request, MASTER_ID, "MASTER");
    }

    private MockHttpServletRequestBuilder authenticated(
            MockHttpServletRequestBuilder request, UUID userId, String role) {
        return request
                .header("X-User-Id", userId)
                .header("X-Role", role);
    }

    private void authenticate(UUID userId, String role) {
        CustomUserDetails principal = CustomUserDetails.from(userId, null, null, role);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }
}
