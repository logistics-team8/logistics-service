package com.logistics.companyproductservice.presentation.controller;

import com.logistics.companyproductservice.domain.model.Company;
import com.logistics.companyproductservice.domain.repository.CompanyRepository;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "spring.config.import=",
        "spring.datasource.url=jdbc:postgresql://localhost:5432/logistics?currentSchema=company_products",
        "spring.datasource.username=logistics",
        "spring.datasource.password=logistics",
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.sql.init.mode=always",
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
@Transactional
@DisplayName("CompanyController 통합 테스트")
class CompanyControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private Filter springSecurityFilterChain;

    @Autowired
    private CompanyRepository companyRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilter(springSecurityFilterChain)
                .build();
    }

    @Test
    @DisplayName("MASTER가 업체를 생성하면 201과 함께 생성된 정보를 반환한다")
    void createCompanySucceeds() throws Exception {
        UUID hubId = UUID.randomUUID();
        String requestBody = """
                {
                  "name": "통합테스트업체",
                  "type": "PRODUCER",
                  "hubId": "%s",
                  "address": "테스트 주소"
                }
                """.formatted(hubId);

        mockMvc.perform(post("/api/v1/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-Role", "MASTER"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("통합테스트업체"))
                .andExpect(jsonPath("$.error").isEmpty());
    }

    @Test
    @DisplayName("이미 존재하는 이름으로 생성하면 409와 DUPLICATE_RESOURCE 에러를 반환한다")
    void createCompanyFailsOnDuplicateName() throws Exception {
        UUID hubId = UUID.randomUUID();
        String requestBody = """
                {
                  "name": "중복테스트업체",
                  "type": "PRODUCER",
                  "hubId": "%s",
                  "address": "테스트 주소"
                }
                """.formatted(hubId);

        mockMvc.perform(post("/api/v1/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-Role", "MASTER"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-Role", "MASTER"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("COMMON_202"));
    }

    @Test
    @DisplayName("HUB_MANAGER가 담당 허브가 아닌 곳으로 생성하면 403과 COMPANY_301 에러를 반환한다")
    void createCompanyFailsWhenHubManagerUsesOtherHub() throws Exception {
        UUID myHubId = UUID.randomUUID();
        UUID otherHubId = UUID.randomUUID();
        String requestBody = """
                {
                  "name": "허브불일치업체",
                  "type": "PRODUCER",
                  "hubId": "%s",
                  "address": "테스트 주소"
                }
                """.formatted(otherHubId);

        mockMvc.perform(post("/api/v1/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-Role", "HUB_MANAGER")
                        .header("X-Hub-Id", myHubId.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("COMPANY_301"));
    }

    @Test
    @DisplayName("존재하지 않는 업체를 조회하면 404를 반환한다")
    void getCompanyReturnsNotFoundForMissingId() throws Exception {
        UUID randomId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/companies/{id}", randomId)
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-Role", "MASTER"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("COMMON_201"));
    }

    @Test
    @DisplayName("존재하는 업체를 조회하면 정상적으로 조회된다")
    void getCompanyReturnsExistingCompany() throws Exception {
        Company company = Company.create("조회테스트업체", Company.Type.RECEIVER, UUID.randomUUID(), "테스트 주소");
        Company saved = companyRepository.saveAndFlush(company);

        mockMvc.perform(get("/api/v1/companies/{id}", saved.getId())
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-Role", "MASTER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("조회테스트업체"));
    }
}