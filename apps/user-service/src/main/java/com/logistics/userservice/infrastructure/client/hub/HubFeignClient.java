package com.logistics.userservice.infrastructure.client.hub;

import com.logistics.common.response.ApiResponse;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "hub-service", path = "/internal/hubs")
public interface HubFeignClient {
    @GetMapping("/{hubId}/exists")
    ApiResponse<HubExistsResponse> checkHubExists(@PathVariable("hubId") UUID hubId);
}
