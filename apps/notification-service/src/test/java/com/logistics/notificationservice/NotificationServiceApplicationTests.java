package com.logistics.notificationservice;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@Disabled("외부 DB 및 Config Server가 필요한 통합 테스트")
@SpringBootTest
@ActiveProfiles("local")
class NotificationServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
