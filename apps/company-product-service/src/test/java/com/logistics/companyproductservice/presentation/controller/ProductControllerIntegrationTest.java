package com.logistics.companyproductservice.presentation.controller;

import com.logistics.companyproductservice.domain.model.Product;
import com.logistics.companyproductservice.domain.repository.ProductRepository;
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

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "spring.config.import=",
        "spring.datasource.url=jdbc:postgresql://localhost:5432/logistics",
        "spring.datasource.username=logistics",
        "spring.datasource.password=logistics",
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.sql.init.mode=always",
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
@Transactional
@DisplayName("ProductController 통합 테스트")
class ProductControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private Filter springSecurityFilterChain;

    @Autowired
    private ProductRepository productRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilter(springSecurityFilterChain)
                .build();
    }

    @Test
    @DisplayName("COMPANY_MANAGER가 본인 소속 업체 상품을 생성하면 201을 반환한다")
    void createProductSucceeds() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        String requestBody = """
                {
                  "name": "통합테스트상품",
                  "companyId": "%s",
                  "hubId": "%s",
                  "unitPrice": 5000
                }
                """.formatted(companyId, hubId);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-Role", "COMPANY_MANAGER")
                        .header("X-Company-Id", companyId.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("통합테스트상품"))
                .andExpect(jsonPath("$.data.stockQuantity").value(0));
    }

    @Test
    @DisplayName("COMPANY_MANAGER가 다른 업체 상품을 생성하려 하면 403과 PROD_302 에러를 반환한다")
    void createProductFailsWhenCompanyManagerUsesOtherCompany() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID otherCompanyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        String requestBody = """
                {
                  "name": "권한없는상품",
                  "companyId": "%s",
                  "hubId": "%s",
                  "unitPrice": 5000
                }
                """.formatted(otherCompanyId, hubId);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-Role", "COMPANY_MANAGER")
                        .header("X-Company-Id", companyId.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PROD_302"));
    }

    @Test
    @DisplayName("존재하지 않는 상품을 조회하면 404를 반환한다")
    void getProductReturnsNotFoundForMissingId() throws Exception {
        UUID randomId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/products/{id}", randomId)
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-Role", "MASTER"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("COMMON_201"));
    }

    @Test
    @DisplayName("HUB_MANAGER가 담당 허브가 아닌 상품을 수정하려 하면 403과 PROD_302 에러를 반환한다")
    void updateProductFailsWhenHubManagerUsesOtherHub() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        Product product = Product.create("수정테스트상품", companyId, hubId, BigDecimal.valueOf(1000));
        Product saved = productRepository.save(product);

        String requestBody = """
                {
                  "name": "수정된이름",
                  "unitPrice": 2000
                }
                """;

        mockMvc.perform(patch("/api/v1/products/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-Role", "HUB_MANAGER")
                        .header("X-Hub-Id", UUID.randomUUID().toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PROD_302"));
    }

    @Test
    @DisplayName("MASTER가 상품을 수정하면 200과 수정된 정보를 반환한다")
    void updateProductSucceedsForMaster() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        Product product = Product.create("수정전이름", companyId, hubId, BigDecimal.valueOf(1000));
        Product saved = productRepository.save(product);

        String requestBody = """
                {
                  "name": "수정후이름",
                  "unitPrice": 3000
                }
                """;

        mockMvc.perform(patch("/api/v1/products/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-Role", "MASTER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("수정후이름"));
    }

    @Test
    @DisplayName("MASTER가 상품을 삭제하면 200을 반환하고 소프트 삭제된다")
    void deleteProductSucceedsForMaster() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        Product product = Product.create("삭제테스트상품", companyId, hubId, BigDecimal.valueOf(1000));
        Product saved = productRepository.save(product);

        mockMvc.perform(delete("/api/v1/products/{id}", saved.getId())
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-Role", "MASTER"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/products/{id}", saved.getId())
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-Role", "MASTER"))
                .andExpect(status().isNotFound());
    }
}