package com.logistics.hubservice.presentation.hub;

import com.logistics.common.response.ApiResponse;
import com.logistics.common.security.CustomUserDetails;
import com.logistics.hubservice.application.hub.command.HubCommandService;
import com.logistics.hubservice.application.hub.query.HubQueryService;
import com.logistics.hubservice.presentation.hub.dto.CreateHubRequest;
import com.logistics.hubservice.presentation.hub.dto.HubResponseDto;
import com.logistics.hubservice.presentation.hub.dto.UpdateHubRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HubController implements HubApi {

    private final HubCommandService hubCommandService;
    private final HubQueryService hubQueryService;

    public HubController(HubCommandService hubCommandService, HubQueryService hubQueryService) {
        this.hubCommandService = hubCommandService;
        this.hubQueryService = hubQueryService;
    }

    @Override
    public ResponseEntity<ApiResponse<HubResponseDto>> create(@Valid @RequestBody CreateHubRequest request) {
        HubResponseDto response = HubResponseDto.from(hubCommandService.create(request.toCommand()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Override
    public ResponseEntity<ApiResponse<HubResponseDto>> getOne(UUID hubId) {
        HubResponseDto response = HubResponseDto.from(hubQueryService.getOne(hubId));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Override
    public ResponseEntity<ApiResponse<List<HubResponseDto>>> getAll() {
        List<HubResponseDto> response = hubQueryService.getAll().stream()
                .map(HubResponseDto::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Override
    public ResponseEntity<ApiResponse<HubResponseDto>> update(
            UUID hubId, @Valid @RequestBody UpdateHubRequest request) {
        HubResponseDto response = HubResponseDto.from(hubCommandService.update(hubId, request.toCommand()));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> delete(
            UUID hubId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        hubCommandService.delete(hubId, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
