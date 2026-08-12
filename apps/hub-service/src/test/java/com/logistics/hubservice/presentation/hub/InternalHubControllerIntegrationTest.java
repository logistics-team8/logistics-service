package com.logistics.hubservice.presentation.hub;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Hub 내부 API 통합 테스트")
class InternalHubControllerIntegrationTest extends PostgreSqlIntegrationTest {

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
    @DisplayName("인증 없이 활성 Hub 존재 여부를 공통 응답으로 조회한다")
    void activeHubExistsWithoutAuthentication() throws Exception {
        Hub hub = saveHub("서울 허브");

        mockMvc.perform(get("/internal/v1/hubs/{hubId}/exists", hub.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exists").value(true))
                .andExpect(jsonPath("$.error").value(nullValue()));
    }

    @Test
    @DisplayName("존재하지 않는 Hub는 존재하지 않는 것으로 응답한다")
    void missingHubDoesNotExist() throws Exception {
        mockMvc.perform(get("/internal/v1/hubs/{hubId}/exists", UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exists").value(false))
                .andExpect(jsonPath("$.error").value(nullValue()));
    }

    @Test
    @DisplayName("논리 삭제된 Hub는 존재하지 않는 것으로 응답한다")
    void deletedHubDoesNotExist() throws Exception {
        Hub hub = saveHub("부산 허브");
        authenticate();
        hub.delete(MASTER_ID);
        hubRepository.save(hub);
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/internal/v1/hubs/{hubId}/exists", hub.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exists").value(false))
                .andExpect(jsonPath("$.error").value(nullValue()));
    }

    @Test
    @DisplayName("Hub 내부 API를 공개 Swagger 문서에서 제외한다")
    void internalApiIsHiddenFromOpenApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/internal/v1/hubs/{hubId}/exists']").doesNotExist());
    }

    private Hub saveHub(String name) {
        authenticate();
        Hub hub = hubRepository.save(Hub.create(
                name,
                "주소",
                new BigDecimal("37.5000000"),
                new BigDecimal("127.0000000")
        ));
        SecurityContextHolder.clearContext();
        return hub;
    }

    private void authenticate() {
        CustomUserDetails principal = CustomUserDetails.from(MASTER_ID, null, null, "MASTER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }
}
