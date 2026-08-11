package com.logistics.hubservice.presentation.hub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
@DisplayName("Hub API 통합 테스트")
class HubControllerIntegrationTest extends PostgreSqlIntegrationTest {

    private static final String HUB_BY_ID_CACHE = "hubById";
    private static final UUID MASTER_ID = UUID.fromString("e81cce60-2e94-41cd-9b89-dbf7dfc5f9b5");
    private static final UUID HUB_MANAGER_ID = UUID.fromString("5136e949-d047-4f31-8da2-e9654dd80f38");
    private static final UUID USER_ID = UUID.fromString("c69b113d-0991-4d8c-b7d0-87bdfadd18ae");

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
    void clearHubsAndRoutes() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(springSecurityFilterChain)
                .build();
        jdbcTemplate.update("delete from p_hub_routes");
        jdbcTemplate.update("delete from p_hubs");
        cacheManager.getCache(HUB_BY_ID_CACHE).clear();
        cacheManager.getCache("hubRouteById").clear();
        cacheManager.getCache("hubRoutePath").clear();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void masterCanCreateHubWithSuccessEnvelope() throws Exception {
        mockMvc.perform(master(post("/api/v1/hubs"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "서울 허브",
                                  "address": "서울특별시 송파구 송파대로 55",
                                  "latitude": 37.5145751,
                                  "longitude": 127.1122451
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.hubId").isNotEmpty())
                .andExpect(jsonPath("$.data.name").value("서울 허브"))
                .andExpect(jsonPath("$.error").value(nullValue()));
    }

    @Test
    void createRejectsInvalidRequestWithValidationEnvelope() throws Exception {
        mockMvc.perform(master(post("/api/v1/hubs"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": " ",
                                  "address": "",
                                  "latitude": 91,
                                  "longitude": 181
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("COMMON_002"));
    }

    @Test
    void nonMasterCannotCreateHub() throws Exception {
        mockMvc.perform(authenticated(post("/api/v1/hubs"), USER_ID, "CUSTOMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "서울 허브",
                                  "address": "서울특별시 송파구 송파대로 55",
                                  "latitude": 37.5145751,
                                  "longitude": 127.1122451
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("COMMON_102"));
    }

    @Test
    void unauthorizedRoleCannotUpdateHub() throws Exception {
        Hub hub = saveHub("서울 허브");

        mockMvc.perform(authenticated(patch("/api/v1/hubs/{hubId}", hub.getId()), USER_ID, "CUSTOMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "동서울 허브" }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("COMMON_102"));
    }

    @Test
    void nonMasterCannotDeleteHub() throws Exception {
        Hub hub = saveHub("서울 허브");

        mockMvc.perform(authenticated(delete("/api/v1/hubs/{hubId}", hub.getId()), USER_ID, "CUSTOMER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("COMMON_102"));
    }

    @Test
    void authenticatedUserCanGetOneHub() throws Exception {
        Hub hub = saveHub("서울 허브");

        mockMvc.perform(authenticated(get("/api/v1/hubs/{hubId}", hub.getId()), USER_ID, "CUSTOMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hubId").value(hub.getId().toString()))
                .andExpect(jsonPath("$.data.name").value("서울 허브"))
                .andExpect(jsonPath("$.error").value(nullValue()));
    }

    @Test
    @DisplayName("단건 조회 결과를 Redis에 JSON으로 1시간 캐싱한다")
    void getOneCachesJsonResponseForOneHour() throws Exception {
        Hub hub = saveHub("서울 허브");

        mockMvc.perform(authenticated(get("/api/v1/hubs/{hubId}", hub.getId()), USER_ID, "CUSTOMER"))
                .andExpect(status().isOk());

        Set<String> cacheKeys = redisTemplate.keys("hubById::*");
        assertThat(cacheKeys).containsExactly(hubCacheKey(hub.getId()));
        String cachedResponse = redisTemplate.opsForValue().get(hubCacheKey(hub.getId()));
        assertThat(cachedResponse)
                .startsWith("{")
                .contains("\"hubId\"")
                .contains("\"name\":\"서울 허브\"");
        assertThat(redisTemplate.getExpire(hubCacheKey(hub.getId()), TimeUnit.SECONDS))
                .isBetween(1L, 3_600L);

        jdbcTemplate.update("delete from p_hubs where id = ?", hub.getId());

        mockMvc.perform(authenticated(get("/api/v1/hubs/{hubId}", hub.getId()), USER_ID, "CUSTOMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hubId").value(hub.getId().toString()))
                .andExpect(jsonPath("$.data.name").value("서울 허브"));
    }

    @Test
    @DisplayName("허브 검색 결과는 Redis에 저장하지 않는다")
    void searchDoesNotCachePageResults() throws Exception {
        saveHub("서울 허브");

        mockMvc.perform(authenticated(get("/api/v1/hubs"), USER_ID, "CUSTOMER"))
                .andExpect(status().isOk());

        assertThat(redisTemplate.keys("hubById::*")).isEmpty();
    }

    @Test
    @DisplayName("인증 사용자는 기본 페이징 조건으로 활성 허브를 조회한다")
    void authenticatedUserCanSearchActiveHubsWithDefaultPaging() throws Exception {
        Hub olderHub = saveHub("서울 허브");
        Hub newerHub = saveHub("부산 허브");
        updateCreatedAt(olderHub, LocalDateTime.of(2026, 8, 8, 9, 0));
        updateCreatedAt(newerHub, LocalDateTime.of(2026, 8, 8, 10, 0));

        mockMvc.perform(authenticated(get("/api/v1/hubs"), USER_ID, "CUSTOMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].hubId").value(newerHub.getId().toString()))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.error").value(nullValue()));
    }

    @Test
    @DisplayName("검색어의 공백과 대소문자를 무시해 허브 이름 또는 주소를 검색한다")
    void authenticatedUserCanSearchByTrimmedKeywordInNameOrAddress() throws Exception {
        saveHub("SEOUL Hub", "Korea");
        saveHub("Busan Hub", "SeOuL Road 55");
        saveHub("Daegu Hub", "Daegu Road 1");

        mockMvc.perform(authenticated(get("/api/v1/hubs"), USER_ID, "CUSTOMER")
                        .param("keyword", "  seOuL  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[*].name", containsInAnyOrder("SEOUL Hub", "Busan Hub")))
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    @DisplayName("허용하지 않는 페이지 크기는 10으로 보정한다")
    void unsupportedPageSizeFallsBackToTen() throws Exception {
        for (int index = 0; index < 12; index++) {
            saveHub("허브 " + index);
        }

        mockMvc.perform(authenticated(get("/api/v1/hubs"), USER_ID, "CUSTOMER")
                        .param("size", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(10))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.totalElements").value(12))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(true));
    }

    @Test
    @DisplayName("생성일과 수정일을 오름차순 또는 내림차순으로 정렬한다")
    void searchSupportsCreatedAtAndUpdatedAtInBothDirections() throws Exception {
        Hub olderHub = saveHub("서울 허브");
        Hub newerHub = saveHub("부산 허브");
        updateCreatedAt(olderHub, LocalDateTime.of(2026, 8, 8, 9, 0));
        updateCreatedAt(newerHub, LocalDateTime.of(2026, 8, 8, 10, 0));
        updateUpdatedAt(olderHub, LocalDateTime.of(2026, 8, 8, 9, 0));
        updateUpdatedAt(newerHub, LocalDateTime.of(2026, 8, 8, 10, 0));

        assertFirstHubForSort("createdAt,asc", olderHub);
        assertFirstHubForSort("createdAt,desc", newerHub);
        assertFirstHubForSort("updatedAt,asc", olderHub);
        assertFirstHubForSort("updatedAt,desc", newerHub);
    }

    @Test
    @DisplayName("지원하지 않는 정렬 필드는 COMMON_001 응답으로 거절한다")
    void searchRejectsUnsupportedSortProperty() throws Exception {
        mockMvc.perform(authenticated(get("/api/v1/hubs"), USER_ID, "CUSTOMER")
                        .param("sort", "name,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("COMMON_001"));
    }

    @Test
    void masterCanUpdateHub() throws Exception {
        Hub hub = saveHub("서울 허브");

        mockMvc.perform(master(patch("/api/v1/hubs/{hubId}", hub.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "동서울 허브" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hubId").value(hub.getId().toString()))
                .andExpect(jsonPath("$.data.name").value("동서울 허브"))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void hubManagerCanUpdateHub() throws Exception {
        Hub hub = saveHub("서울 허브");

        mockMvc.perform(authenticated(
                        patch("/api/v1/hubs/{hubId}", hub.getId()), HUB_MANAGER_ID, "HUB_MANAGER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "동서울 허브" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hubId").value(hub.getId().toString()))
                .andExpect(jsonPath("$.data.name").value("동서울 허브"))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    @DisplayName("허브를 수정하면 단건 조회 캐시를 제거하고 변경된 정보를 다시 캐싱한다")
    void updateEvictsHubCache() throws Exception {
        Hub hub = saveHub("서울 허브");
        mockMvc.perform(authenticated(get("/api/v1/hubs/{hubId}", hub.getId()), USER_ID, "CUSTOMER"))
                .andExpect(status().isOk());
        assertThat(redisTemplate.hasKey(hubCacheKey(hub.getId()))).isTrue();

        mockMvc.perform(master(patch("/api/v1/hubs/{hubId}", hub.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "동서울 허브" }
                                """))
                .andExpect(status().isOk());

        assertThat(redisTemplate.hasKey(hubCacheKey(hub.getId()))).isFalse();
        mockMvc.perform(authenticated(get("/api/v1/hubs/{hubId}", hub.getId()), USER_ID, "CUSTOMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("동서울 허브"));
        assertThat(redisTemplate.hasKey(hubCacheKey(hub.getId()))).isTrue();
    }

    @Test
    void masterCannotUpdateHubWithoutAnyFields() throws Exception {
        Hub hub = saveHub("서울 허브");

        mockMvc.perform(master(patch("/api/v1/hubs/{hubId}", hub.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("COMMON_002"));
    }

    @Test
    @DisplayName("MASTER가 허브를 삭제하면 연결된 활성 경로에도 같은 삭제 정보를 기록한다")
    void masterCanDeleteHubAndItsActiveRoutes() throws Exception {
        Hub hub = saveHub("서울 허브");
        Hub otherHub = saveHub("대전 허브");
        Hub thirdHub = saveHub("부산 허브");
        HubRoute outgoingRoute = saveRoute(hub.getId(), otherHub.getId());
        HubRoute incomingRoute = saveRoute(thirdHub.getId(), hub.getId());
        HubRoute unrelatedRoute = saveRoute(otherHub.getId(), thirdHub.getId());

        mockMvc.perform(master(delete("/api/v1/hubs/{hubId}", hub.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error").value(nullValue()));

        assertDeletedBy("p_hubs", hub.getId(), MASTER_ID);
        assertDeletedBy("p_hub_routes", outgoingRoute.getId(), MASTER_ID);
        assertDeletedBy("p_hub_routes", incomingRoute.getId(), MASTER_ID);
        assertThat(jdbcTemplate.queryForObject(
                "select deleted_at is null from p_hub_routes where id = ?",
                Boolean.class,
                unrelatedRoute.getId()))
                .isTrue();

        mockMvc.perform(authenticated(get("/api/v1/hubs/{hubId}", hub.getId()), USER_ID, "CUSTOMER"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("HUB_001"))
                .andExpect(jsonPath("$.error.message").value("허브를 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("허브를 삭제하면 연결 경로의 단건 캐시만 제거하고 모든 최단 경로 캐시를 제거한다")
    void deleteHubEvictsConnectedRouteCachesAndAllPathCaches() throws Exception {
        Hub hub = saveHub("서울 허브");
        Hub otherHub = saveHub("대전 허브");
        Hub thirdHub = saveHub("부산 허브");
        HubRoute connectedRoute = saveRoute(hub.getId(), otherHub.getId());
        HubRoute unrelatedRoute = saveRoute(otherHub.getId(), thirdHub.getId());

        mockMvc.perform(authenticated(
                        get("/api/v1/hub-routes/{hubRouteId}", connectedRoute.getId()),
                        HUB_MANAGER_ID,
                        "HUB_MANAGER"))
                .andExpect(status().isOk());
        mockMvc.perform(authenticated(
                        get("/api/v1/hub-routes/{hubRouteId}", unrelatedRoute.getId()),
                        HUB_MANAGER_ID,
                        "HUB_MANAGER"))
                .andExpect(status().isOk());
        redisTemplate.opsForValue().set("hubRoutePath::seoul-busan", "{}");
        redisTemplate.opsForValue().set("hubRoutePath::incheon-daegu", "{}");

        mockMvc.perform(master(delete("/api/v1/hubs/{hubId}", hub.getId())))
                .andExpect(status().isOk());

        assertThat(redisTemplate.hasKey("hubRouteById::" + connectedRoute.getId())).isFalse();
        assertThat(redisTemplate.hasKey("hubRouteById::" + unrelatedRoute.getId())).isTrue();
        assertThat(redisTemplate.keys("hubRoutePath::*")).isEmpty();
    }

    @Test
    void deletingSameHubTwiceReturnsHub001() throws Exception {
        Hub hub = saveHub("서울 허브");

        mockMvc.perform(master(delete("/api/v1/hubs/{hubId}", hub.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(master(delete("/api/v1/hubs/{hubId}", hub.getId())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("HUB_001"))
                .andExpect(jsonPath("$.error.message").value("허브를 찾을 수 없습니다."));
    }

    @Test
    void missingHubReturnsHub001() throws Exception {
        UUID missingHubId = UUID.fromString("6f21f2ae-d913-45db-83e5-1a5695536171");

        mockMvc.perform(authenticated(get("/api/v1/hubs/{hubId}", missingHubId), USER_ID, "CUSTOMER"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("HUB_001"))
                .andExpect(jsonPath("$.error.message").value("허브를 찾을 수 없습니다."));
    }

    @Test
    void unauthenticatedReadIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/hubs"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("COMMON_101"));
    }

    @Test
    @DisplayName("Swagger에 허브 삭제 시 연결된 활성 경로도 삭제한다는 설명을 공개한다")
    void openApiDocumentIsPubliclyAvailable() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/hubs']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/hubs'].get.parameters[*].name", hasItem("keyword")))
                .andExpect(jsonPath("$.paths['/api/v1/hubs'].get.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/hubs/{hubId}']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/hubs/{hubId}'].patch.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/hubs/{hubId}'].delete.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/hubs/{hubId}'].delete.description")
                        .value(containsString("연결된 활성 경로")));
    }

    @Test
    void actuatorHealthIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
    }

    @Test
    void swaggerUiIsPublic() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(result -> assertThat(result.getResponse().getStatus())
                        .isIn(200, 302)
                        .isNotIn(401, 403));
    }

    private Hub saveHub(String name) {
        return saveHub(name, "서울특별시 송파구 송파대로 55");
    }

    private Hub saveHub(String name, String address) {
        CustomUserDetails principal = CustomUserDetails.from(MASTER_ID, null, null, "MASTER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
        Hub hub = hubRepository.save(Hub.create(
                name,
                address,
                new BigDecimal("37.5145751"),
                new BigDecimal("127.1122451")
        ));
        SecurityContextHolder.clearContext();
        return hub;
    }

    private String hubCacheKey(UUID hubId) {
        return HUB_BY_ID_CACHE + "::" + hubId;
    }

    private HubRoute saveRoute(UUID sourceHubId, UUID destinationHubId) {
        CustomUserDetails principal = CustomUserDetails.from(MASTER_ID, null, null, "MASTER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
        HubRoute route = hubRouteRepository.save(HubRoute.create(
                sourceHubId,
                destinationHubId,
                123_400L,
                7_200L
        ));
        SecurityContextHolder.clearContext();
        return route;
    }

    private void assertDeletedBy(String tableName, UUID id, UUID deletedBy) {
        assertThat(jdbcTemplate.queryForObject(
                "select deleted_at is not null from " + tableName + " where id = ?",
                Boolean.class,
                id))
                .isTrue();
        assertThat(jdbcTemplate.queryForObject(
                "select deleted_by from " + tableName + " where id = ?",
                UUID.class,
                id))
                .isEqualTo(deletedBy);
    }

    private void updateCreatedAt(Hub hub, LocalDateTime createdAt) {
        jdbcTemplate.update("update p_hubs set created_at = ? where id = ?", createdAt, hub.getId());
    }

    private void updateUpdatedAt(Hub hub, LocalDateTime updatedAt) {
        jdbcTemplate.update("update p_hubs set updated_at = ? where id = ?", updatedAt, hub.getId());
    }

    private void assertFirstHubForSort(String sort, Hub expectedHub) throws Exception {
        mockMvc.perform(authenticated(get("/api/v1/hubs"), USER_ID, "CUSTOMER")
                        .param("sort", sort))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].hubId").value(expectedHub.getId().toString()));
    }

    private MockHttpServletRequestBuilder master(MockHttpServletRequestBuilder request) {
        return authenticated(request, MASTER_ID, "MASTER");
    }

    private MockHttpServletRequestBuilder authenticated(
            MockHttpServletRequestBuilder request, UUID userId, String authority) {
        return request
                .header("X-User-Id", userId)
                .header("X-Role", authority);
    }
}
