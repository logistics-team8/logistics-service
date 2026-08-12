package com.logistics.hubservice.presentation.hub;

import com.logistics.common.response.ApiResponse;
import com.logistics.hubservice.application.hub.query.HubQueryService;
import com.logistics.hubservice.presentation.hub.dto.HubExistsResponse;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/hubs")
public class InternalHubController {

    private final HubQueryService hubQueryService;

    @GetMapping("/{hubId}/exists")
    public ResponseEntity<ApiResponse<HubExistsResponse>> exists(@PathVariable UUID hubId) {
        HubExistsResponse response = new HubExistsResponse(hubQueryService.exists(hubId));
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
