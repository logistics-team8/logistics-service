package com.logistics.deliveryservice.infrastructure.client.hub;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.deliveryservice.DeliveryServiceApplication;
import com.logistics.deliveryservice.infrastructure.client.hub.dto.HubDeliveryPlanRequest;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

class HubDeliveryPlanFeignClientTest {

    @Test
    void declaresHubServicePostContract() throws NoSuchMethodException {
        FeignClient feignClient = HubDeliveryPlanFeignClient.class.getAnnotation(FeignClient.class);
        Method method = HubDeliveryPlanFeignClient.class.getDeclaredMethod(
                "createDeliveryPlan",
                HubDeliveryPlanRequest.class
        );
        PostMapping postMapping = method.getAnnotation(PostMapping.class);

        assertThat(feignClient.name()).isEqualTo("hub-service");
        assertThat(postMapping.value()).containsExactly("/internal/v1/delivery-plans");
        assertThat(method.getParameterAnnotations()[0])
                .anyMatch(annotation -> annotation.annotationType() == RequestBody.class);
        assertThat(DeliveryServiceApplication.class.getAnnotation(EnableFeignClients.class))
                .isNotNull();
    }
}
