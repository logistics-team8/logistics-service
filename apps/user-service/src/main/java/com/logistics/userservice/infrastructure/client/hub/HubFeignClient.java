package com.logistics.userservice.infrastructure.client.hub;

import com.logistics.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "hub-service", path = "/internal/v1/hubs")
public interface HubFeignClient {
    @GetMapping("/{hubId}/exists")
    ApiResponse<HubExistsResponse> checkHubExists(@PathVariable("hubId") UUID hubId);
}
