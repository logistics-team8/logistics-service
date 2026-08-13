package com.logistics.companyproductservice.presentation.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.logistics.companyproductservice.application.service.CompanyService;
import com.logistics.companyproductservice.application.service.ProductService;
import com.logistics.companyproductservice.infrastructure.config.SwaggerConfig;
import org.junit.jupiter.api.Test;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.configuration.SpringDocPageableConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({CompanyController.class, ProductController.class})
@AutoConfigureMockMvc(addFilters = false)
@Import(SwaggerConfig.class)
@ImportAutoConfiguration({
        SpringDocConfiguration.class,
        SpringDocConfigProperties.class,
        SpringDocPageableConfiguration.class,
        SpringDocWebMvcConfiguration.class
})
class CompanyProductOpenApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CompanyService companyService;

    @MockitoBean
    private ProductService productService;

    @Test
    void exposesCompanyAndProductApiDocumentation() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/companies']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/products']").exists());
    }
}
