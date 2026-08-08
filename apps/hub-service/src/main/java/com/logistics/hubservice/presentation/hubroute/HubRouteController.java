package com.logistics.hubservice.presentation.hubroute;

import com.logistics.common.response.ApiResponse;
import com.logistics.hubservice.application.hubroute.command.HubRouteCommandService;
import com.logistics.hubservice.presentation.hubroute.dto.CreateHubRouteRequest;
import com.logistics.hubservice.presentation.hubroute.dto.HubRouteResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HubRouteController implements HubRouteApi {

    private final HubRouteCommandService hubRouteCommandService;

    @Override
    public ResponseEntity<ApiResponse<HubRouteResponseDto>> create(
            @Valid @RequestBody CreateHubRouteRequest request) {
        HubRouteResponseDto response = HubRouteResponseDto.from(
                hubRouteCommandService.create(request.toCommand()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }
}
