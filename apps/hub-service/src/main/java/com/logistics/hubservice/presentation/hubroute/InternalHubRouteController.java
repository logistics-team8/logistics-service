package com.logistics.hubservice.presentation.hubroute;

import com.logistics.common.response.ApiResponse;
import com.logistics.hubservice.application.hubroute.query.HubRouteQueryService;
import com.logistics.hubservice.presentation.hubroute.dto.HubRoutePathResponseDto;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/hub-routes")
public class InternalHubRouteController {

    private final HubRouteQueryService hubRouteQueryService;

    @GetMapping("/shortest-path")
    public ResponseEntity<ApiResponse<HubRoutePathResponseDto>> getShortestPath(
            @RequestParam("sourceHubId") UUID sourceHubId,
            @RequestParam("destinationHubId") UUID destinationHubId) {
        HubRoutePathResponseDto response = HubRoutePathResponseDto.from(
                hubRouteQueryService.getShortestPath(sourceHubId, destinationHubId));
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
