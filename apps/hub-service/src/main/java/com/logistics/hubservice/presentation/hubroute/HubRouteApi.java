package com.logistics.hubservice.presentation.hubroute;

import com.logistics.common.response.ApiResponse;
import com.logistics.hubservice.presentation.hubroute.dto.CreateHubRouteRequest;
import com.logistics.hubservice.presentation.hubroute.dto.HubRouteResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Hub Route", description = "허브 간 이동 경로 관리 API")
@RequestMapping("/api/v1/hub-routes")
public interface HubRouteApi {

    @Operation(
            summary = "허브 경로 생성",
            description = "MASTER 권한이 필요합니다. 출발·도착 허브는 활성 상태여야 하며 서로 달라야 합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 또는 동일 허브 경로"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "MASTER 권한 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "허브 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "활성 경로 중복")
    })
    @PostMapping
    ResponseEntity<ApiResponse<HubRouteResponseDto>> create(
            @Valid @RequestBody CreateHubRouteRequest request);

    @Operation(
            summary = "허브 경로 단건 조회",
            description = "인증이 필요합니다. 삭제되었거나 존재하지 않는 경로는 HUB_002로 응답합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "허브 경로 없음")
    })
    @GetMapping("/{hubRouteId}")
    ResponseEntity<ApiResponse<HubRouteResponseDto>> getOne(
            @PathVariable UUID hubRouteId);
}
