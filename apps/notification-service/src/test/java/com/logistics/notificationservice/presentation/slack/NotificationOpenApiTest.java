package com.logistics.notificationservice.presentation.slack;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.logistics.notificationservice.application.slack.SlackMessageService;
import com.logistics.notificationservice.infrastructure.config.SwaggerConfig;
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

@WebMvcTest(SlackMessageController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SwaggerConfig.class)
@ImportAutoConfiguration({
        SpringDocConfiguration.class,
        SpringDocConfigProperties.class,
        SpringDocPageableConfiguration.class,
        SpringDocWebMvcConfiguration.class
})
class NotificationOpenApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SlackMessageService slackMessageService;

    @Test
    void exposesNotificationApiDocumentation() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/slack-messages']").exists());
    }
}
