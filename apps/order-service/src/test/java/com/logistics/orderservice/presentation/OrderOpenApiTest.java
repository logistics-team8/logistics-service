package com.logistics.orderservice.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.logistics.orderservice.application.service.command.OrderCancelService;
import com.logistics.orderservice.application.service.command.OrderCreateService;
import com.logistics.orderservice.application.service.command.OrderManagementService;
import com.logistics.orderservice.application.service.query.OrderQueryService;
import org.junit.jupiter.api.Test;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.configuration.SpringDocPageableConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration({
        SpringDocConfiguration.class,
        SpringDocConfigProperties.class,
        SpringDocPageableConfiguration.class,
        SpringDocWebMvcConfiguration.class
})
class OrderOpenApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderManagementService orderManagementService;

    @MockitoBean
    private OrderCreateService orderCreateService;

    @MockitoBean
    private OrderCancelService orderCancelService;

    @MockitoBean
    private OrderQueryService orderQueryService;

    @Test
    void exposesOrderApiDocumentation() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/orders']").exists());
    }
}
