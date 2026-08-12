package com.logistics.userservice.infrastructure.client.delivery;

import com.logistics.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// TODO : Delevery 담당자와 경로 논의해봐야함
@FeignClient(name = "delivery-service", path = "/internal/v1/deliveries")
public interface DeliveryFeignClient {
    @PostMapping("/managers")
    ApiResponse<Void> createDeliveryManager(@RequestBody CreateDeliveryManagerRequest request);
}
