package com.logistics.hubservice.presentation.hubroute;

import com.logistics.common.response.ApiResponse;
import com.logistics.common.response.PageResponse;
import com.logistics.common.security.principal.CustomUserDetails;
import com.logistics.hubservice.application.hubroute.command.HubRouteCommandService;
import com.logistics.hubservice.application.hubroute.query.HubRouteQueryService;
import com.logistics.hubservice.presentation.hubroute.dto.CreateHubRouteRequest;
import com.logistics.hubservice.presentation.hubroute.dto.HubRoutePathResponseDto;
import com.logistics.hubservice.presentation.hubroute.dto.HubRouteResponseDto;
import com.logistics.hubservice.presentation.hubroute.dto.UpdateHubRouteRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HubRouteController implements HubRouteApi {

    private final HubRouteCommandService hubRouteCommandService;
    private final HubRouteQueryService hubRouteQueryService;

    @Override
    public ResponseEntity<ApiResponse<HubRouteResponseDto>> create(
            @Valid @RequestBody CreateHubRouteRequest request) {
        HubRouteResponseDto response = HubRouteResponseDto.from(
                hubRouteCommandService.create(request.toCommand()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Override
    public ResponseEntity<ApiResponse<PageResponse<HubRouteResponseDto>>> search(
            @RequestParam(required = false) UUID sourceHubId,
            @RequestParam(required = false) UUID destinationHubId,
            Pageable pageable) {
        Page<HubRouteResponseDto> responsePage = hubRouteQueryService
                .search(sourceHubId, destinationHubId, pageable)
                .map(HubRouteResponseDto::from);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(responsePage)));
    }

    @Override
    public ResponseEntity<ApiResponse<HubRoutePathResponseDto>> getShortestPath(
            UUID sourceHubId, UUID destinationHubId) {
        HubRoutePathResponseDto response = HubRoutePathResponseDto.from(
                hubRouteQueryService.getShortestPath(sourceHubId, destinationHubId));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Override
    public ResponseEntity<ApiResponse<HubRouteResponseDto>> getOne(UUID hubRouteId) {
        HubRouteResponseDto response = HubRouteResponseDto.from(
                hubRouteQueryService.getOne(hubRouteId));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Override
    public ResponseEntity<ApiResponse<HubRouteResponseDto>> update(
            UUID hubRouteId, @Valid @RequestBody UpdateHubRouteRequest request) {
        HubRouteResponseDto response = HubRouteResponseDto.from(
                hubRouteCommandService.update(hubRouteId, request.toCommand()));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> delete(
            UUID hubRouteId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        hubRouteCommandService.delete(hubRouteId, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
