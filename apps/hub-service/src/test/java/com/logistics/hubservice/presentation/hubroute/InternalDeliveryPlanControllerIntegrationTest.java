package com.logistics.hubservice.presentation.hubroute;

import static org.hamcrest.Matchers.hasSize;
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
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Hub 내부 배송 계획 API 통합 테스트")
class InternalDeliveryPlanControllerIntegrationTest extends PostgreSqlIntegrationTest {

    private static final UUID MASTER_ID =
            UUID.fromString("e81cce60-2e94-41cd-9b89-dbf7dfc5f9b5");
    private static final UUID ORDER_ID =
            UUID.fromString("09cf5d6c-ec32-43f9-871c-e5f152aa17e0");

    private MockMvc mockMvc;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

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
    @DisplayName("인증 없이 Delivery 계약 경로로 최단 경로 배송 계획을 반환한다")
    void createsDeliveryPlanWithoutAuthentication() throws Exception {
        Hub sourceHub = saveHub("서울 허브");
        Hub intermediateHub = saveHub("대전 허브");
        Hub destinationHub = saveHub("부산 허브");
        saveRoute(sourceHub.getId(), intermediateHub.getId(), 40L, 50L);
        saveRoute(intermediateHub.getId(), destinationHub.getId(), 60L, 70L);
        saveRoute(sourceHub.getId(), destinationHub.getId(), 80L, 200L);

        mockMvc.perform(post("/internal/v1/delivery-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(sourceHub.getId(), destinationHub.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyDeliveryManagerId").doesNotExist())
                .andExpect(jsonPath("$.routes", hasSize(2)))
                .andExpect(jsonPath("$.routes[0].sequence").value(1))
                .andExpect(jsonPath("$.routes[0].departureHubId").value(sourceHub.getId().toString()))
                .andExpect(jsonPath("$.routes[0].arrivalHubId")
                        .value(intermediateHub.getId().toString()))
                .andExpect(jsonPath("$.routes[0].estimatedDistanceKm").value(0.040))
                .andExpect(jsonPath("$.routes[0].estimatedDurationMinutes").value(1))
                .andExpect(jsonPath("$.routes[0].hubDeliveryManagerId").doesNotExist())
                .andExpect(jsonPath("$.routes[1].sequence").value(2))
                .andExpect(jsonPath("$.routes[1].departureHubId")
                        .value(intermediateHub.getId().toString()))
                .andExpect(jsonPath("$.routes[1].arrivalHubId")
                        .value(destinationHub.getId().toString()))
                .andExpect(jsonPath("$.routes[1].estimatedDistanceKm").value(0.060))
                .andExpect(jsonPath("$.routes[1].estimatedDurationMinutes").value(2))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    @DisplayName("같은 허브면 빈 경로 목록을 반환한다")
    void sameHubReturnsEmptyRoutes() throws Exception {
        Hub hub = saveHub("서울 허브");

        mockMvc.perform(post("/internal/v1/delivery-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(hub.getId(), hub.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routes", hasSize(0)));
    }

    @Test
    @DisplayName("출발 허브나 도착 허브가 없으면 404를 반환한다")
    void missingHubReturnsHub001() throws Exception {
        Hub sourceHub = saveHub("서울 허브");

        mockMvc.perform(post("/internal/v1/delivery-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(sourceHub.getId(), UUID.randomUUID())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("HUB_001"));
    }

    @Test
    @DisplayName("활성 허브 사이에 연결 경로가 없으면 404를 반환한다")
    void unreachableDestinationReturnsHub005() throws Exception {
        Hub sourceHub = saveHub("서울 허브");
        Hub destinationHub = saveHub("부산 허브");

        mockMvc.perform(post("/internal/v1/delivery-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(sourceHub.getId(), destinationHub.getId())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("HUB_005"));
    }

    @Test
    @DisplayName("필수 허브 ID가 없으면 400을 반환한다")
    void missingRequiredHubIdReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/internal/v1/delivery-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":"%s","departureHubId":"%s"}
                                """.formatted(ORDER_ID, UUID.randomUUID())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("내부 배송 계획 API를 공개 Swagger 문서에서 제외한다")
    void internalApiIsHiddenFromOpenApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/internal/v1/delivery-plans']").doesNotExist());
    }

    private String requestBody(UUID departureHubId, UUID arrivalHubId) {
        return jsonMapper.writeValueAsString(new DeliveryPlanRequestBody(
                ORDER_ID, departureHubId, arrivalHubId));
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

    private void authenticate() {
        CustomUserDetails principal = CustomUserDetails.from(MASTER_ID, null, null, "MASTER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()));
    }

    private record DeliveryPlanRequestBody(
            UUID orderId,
            UUID departureHubId,
            UUID arrivalHubId
    ) {
    }
}
