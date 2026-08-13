package com.logistics.hubservice.presentation.hub;

import com.logistics.common.response.ApiResponse;
import com.logistics.common.response.PageResponse;
import com.logistics.common.security.principal.CustomUserDetails;
import com.logistics.hubservice.application.hub.command.HubCommandService;
import com.logistics.hubservice.application.hub.query.HubQueryService;
import com.logistics.hubservice.presentation.hub.dto.CreateHubRequest;
import com.logistics.hubservice.presentation.hub.dto.HubResponseDto;
import com.logistics.hubservice.presentation.hub.dto.UpdateHubRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

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
    public ResponseEntity<ApiResponse<PageResponse<HubResponseDto>>> search(
            @RequestParam(required = false) String keyword,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<HubResponseDto> responsePage = hubQueryService.search(keyword, pageable)
                .map(HubResponseDto::from);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(responsePage)));
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
        hubCommandService.delete(hubId, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
