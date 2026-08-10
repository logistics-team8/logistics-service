package com.logistics.hubservice.presentation.hubroute;

import com.logistics.common.response.ApiResponse;
import com.logistics.common.response.PageResponse;
import com.logistics.common.security.principal.CustomUserDetails;
import com.logistics.hubservice.presentation.hubroute.dto.CreateHubRouteRequest;
import com.logistics.hubservice.presentation.hubroute.dto.HubRoutePathResponseDto;
import com.logistics.hubservice.presentation.hubroute.dto.HubRouteResponseDto;
import com.logistics.hubservice.presentation.hubroute.dto.UpdateHubRouteRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Hub Route", description = "허브 간 이동 경로 관리 API")
@RequestMapping("/api/v1/hub-routes")
public interface HubRouteApi {

    @Operation(
            summary = "허브 경로 생성",
            description = "MASTER 권한이 필요합니다. 출발 허브와 도착 허브는 활성 상태여야 하며 서로 달라야 합니다.")
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
            summary = "허브 경로 검색",
            description = "인증이 필요합니다. 출발 허브와 도착 허브 ID를 정확히 일치하는 조건으로 선택해 검색할 수 있습니다. "
                    + "삭제된 경로는 제외하며 페이지 크기는 10, 30, 50만 허용하고 정렬 필드는 createdAt, updatedAt만 사용할 수 있습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "검색 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "허용하지 않는 정렬 필드"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping
    ResponseEntity<ApiResponse<PageResponse<HubRouteResponseDto>>> search(
            @Parameter(description = "출발 허브 UUID")
            @RequestParam(required = false) UUID sourceHubId,
            @Parameter(description = "도착 허브 UUID")
            @RequestParam(required = false) UUID destinationHubId,
            @ParameterObject
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable);

    @Operation(
            summary = "허브 최단 경로 조회",
            description = "인증이 필요합니다. 활성 허브 경로를 소요시간 우선으로 탐색하고 소요시간 합계가 같으면 거리 합계를 비교합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "허브 또는 연결 경로 없음")
    })
    @GetMapping("/shortest-path")
    ResponseEntity<ApiResponse<HubRoutePathResponseDto>> getShortestPath(
            @Parameter(description = "출발 허브 UUID", required = true)
            @RequestParam UUID sourceHubId,
            @Parameter(description = "도착 허브 UUID", required = true)
            @RequestParam UUID destinationHubId);

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

    @Operation(
            summary = "허브 경로 수정",
            description = "MASTER 권한이 필요합니다. 이동 거리와 소요 시간 중 하나 이상을 보내야 하며 전달한 필드만 변경됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "MASTER 권한 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "허브 경로 없음")
    })
    @PatchMapping("/{hubRouteId}")
    ResponseEntity<ApiResponse<HubRouteResponseDto>> update(
            @PathVariable UUID hubRouteId,
            @Valid @RequestBody UpdateHubRouteRequest request);

    @Operation(
            summary = "허브 경로 삭제",
            description = "MASTER 권한이 필요합니다. 활성 경로를 논리 삭제하고 요청자와 삭제 시각을 기록합니다. "
                    + "존재하지 않거나 이미 삭제된 경로는 HUB_002로 응답합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "MASTER 권한 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "허브 경로 없음")
    })
    @DeleteMapping("/{hubRouteId}")
    ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID hubRouteId,
            @AuthenticationPrincipal CustomUserDetails userDetails);
}
