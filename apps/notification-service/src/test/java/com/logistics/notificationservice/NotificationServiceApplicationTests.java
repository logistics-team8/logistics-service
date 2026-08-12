package com.logistics.notificationservice;

import static org.assertj.core.api.Assertions.assertThat;

import feign.micrometer.MicrometerObservationCapability;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.FeignClientFactory;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local")
class NotificationServiceApplicationTests {

    @Autowired private FeignClientFactory feignClientFactory;

    @Test
    void configuresMicrometerObservationForFeignClients() {
        MicrometerObservationCapability capability = feignClientFactory.getInstance(
                "user-service", MicrometerObservationCapability.class);

        assertThat(capability).isNotNull();
    }
}
