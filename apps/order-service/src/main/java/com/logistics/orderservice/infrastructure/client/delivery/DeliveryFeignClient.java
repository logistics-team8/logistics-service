package com.logistics.orderservice.infrastructure.client.delivery;


import com.logistics.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDateTime;
import java.util.UUID;

@FeignClient(
        name = "delivery-service",
        path = "/internal/v1/deliveries"
)
public interface DeliveryFeignClient {
    @PostMapping
    ApiResponse<CreateDeliveryResponse> createDelivery(
      @RequestBody CreateDeliveryRequest request
    );

    record CreateDeliveryRequest(
            UUID orderId,
            UUID requesterId,
            UUID departureHubId,
            UUID arrivalHubId,
            String deliveryAddress,
            String receiverName,
            String receiverSlackId
    ){
    }
    record CreateDeliveryResponse(
            UUID deliveryId,
            UUID orderId,
            UUID requesterId,
            String status,
            UUID departureHubId,
            UUID arrivalHubId,
            String deliveryAddress,
            String receiverName,
            String receiverSlackId
    ) {
    }
}
