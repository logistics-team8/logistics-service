package com.logistics.hubservice;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.hubservice.application.hubroute.initialization.HubRouteDefaultDataInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;

@SpringBootTest(properties = {
        "hub-route.default-data.enabled=true",
        "naver.maps.base-url=https://naver.example",
        "naver.maps.api-key-id=client-id",
        "naver.maps.api-key=client-secret"
})
@ActiveProfiles("test")
class HubServiceApplicationTests extends PostgreSqlIntegrationTest {

    @MockitoBean
    private HubRouteDefaultDataInitializer defaultDataInitializer;

    @Autowired
    private RestClient.Builder restClientBuilder;

    @Test
    void contextLoads() {
        assertThat(restClientBuilder).isNotNull();
    }

}
