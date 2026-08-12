package com.logistics.deliveryservice.infrastructure.client.hub;

import com.logistics.deliveryservice.infrastructure.client.hub.dto.HubDeliveryPlanRequest;
import com.logistics.deliveryservice.infrastructure.client.hub.dto.HubDeliveryPlanResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Eureka에서 hub-service를 찾아 전체 배송 계획을 요청한다.
 */
@FeignClient(name = "hub-service")
public interface HubDeliveryPlanFeignClient {

    @PostMapping("/internal/v1/delivery-plans")
    HubDeliveryPlanResponse createDeliveryPlan(@RequestBody HubDeliveryPlanRequest request);
}
