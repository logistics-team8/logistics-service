package com.logistics.hubservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class HubServiceApplicationTests extends PostgreSqlIntegrationTest {

    @Test
    void contextLoads() {
    }

}
