package com.logistics.hubservice.presentation.hub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.logistics.common.security.CustomUserDetails;
import com.logistics.hubservice.domain.hub.Hub;
import com.logistics.hubservice.domain.hub.HubRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
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
class HubControllerIntegrationTest {

    private static final UUID MASTER_ID = UUID.fromString("e81cce60-2e94-41cd-9b89-dbf7dfc5f9b5");
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
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearHubs() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(springSecurityFilterChain)
                .build();
        jdbcTemplate.update("delete from p_hubs");
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
    void nonMasterCannotUpdateHub() throws Exception {
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
    void authenticatedUserCanGetAllActiveHubs() throws Exception {
        saveHub("서울 허브");
        saveHub("부산 허브");

        mockMvc.perform(authenticated(get("/api/v1/hubs"), USER_ID, "CUSTOMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.error").value(nullValue()));
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
    void masterCanDeleteHubWithNullSuccessEnvelopeAndDeletedHubReturnsHub001() throws Exception {
        Hub hub = saveHub("서울 허브");

        mockMvc.perform(master(delete("/api/v1/hubs/{hubId}", hub.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error").value(nullValue()));

        mockMvc.perform(authenticated(get("/api/v1/hubs/{hubId}", hub.getId()), USER_ID, "CUSTOMER"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("HUB_001"))
                .andExpect(jsonPath("$.error.message").value("허브를 찾을 수 없습니다."));
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
    void openApiDocumentIsPubliclyAvailable() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/hubs']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/hubs/{hubId}']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/hubs/{hubId}'].patch.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/hubs/{hubId}'].delete.responses['401']").exists());
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
        CustomUserDetails principal = CustomUserDetails.from(MASTER_ID, "MASTER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
        Hub hub = hubRepository.save(Hub.create(
                name,
                "서울특별시 송파구 송파대로 55",
                new BigDecimal("37.5145751"),
                new BigDecimal("127.1122451")
        ));
        SecurityContextHolder.clearContext();
        return hub;
    }

    private MockHttpServletRequestBuilder master(MockHttpServletRequestBuilder request) {
        return authenticated(request, MASTER_ID, "MASTER");
    }

    private MockHttpServletRequestBuilder authenticated(
            MockHttpServletRequestBuilder request, UUID userId, String authority) {
        return request
                .header("X-User-Id", userId)
                .header("X-User-Role", authority);
    }
}
