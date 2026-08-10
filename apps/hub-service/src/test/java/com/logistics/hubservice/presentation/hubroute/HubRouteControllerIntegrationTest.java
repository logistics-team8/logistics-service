package com.logistics.hubservice.presentation.hubroute;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.logistics.common.security.principal.CustomUserDetails;
import com.logistics.hubservice.PostgreSqlIntegrationTest;
import com.logistics.hubservice.domain.hub.Hub;
import com.logistics.hubservice.domain.hub.HubRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
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
@DisplayName("HubRoute 생성 API 통합 테스트")
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
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(springSecurityFilterChain)
                .build();
        jdbcTemplate.update("delete from p_hub_routes");
        jdbcTemplate.update("delete from p_hubs");
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
    @DisplayName("출발 또는 도착 허브가 존재하지 않으면 허브를 찾을 수 없다")
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
    @DisplayName("Swagger 문서에 허브 경로 생성 API와 오류 응답을 포함한다")
    void openApiDocumentsTheHubRouteCreateEndpoint() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/hub-routes'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/hub-routes'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/hub-routes'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/hub-routes'].post.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/hub-routes'].post.responses['403']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/hub-routes'].post.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/hub-routes'].post.responses['409']").exists());
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
