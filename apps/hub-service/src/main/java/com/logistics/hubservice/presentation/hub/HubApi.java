package com.logistics.hubservice.presentation.hub;

import com.logistics.common.response.ApiResponse;
import com.logistics.common.security.CustomUserDetails;
import com.logistics.hubservice.presentation.hub.dto.CreateHubRequest;
import com.logistics.hubservice.presentation.hub.dto.HubResponseDto;
import com.logistics.hubservice.presentation.hub.dto.UpdateHubRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Hub", description = "허브 관리 API")
@RequestMapping("/api/v1/hubs")
public interface HubApi {

    @Operation(summary = "허브 생성", description = "MASTER 권한이 필요합니다. 이름, 주소, 위도, 경도는 요청 본문의 검증 규칙을 충족해야 합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "MASTER 권한 필요")
    })
    @PostMapping
    ResponseEntity<ApiResponse<HubResponseDto>> create(@Valid @RequestBody CreateHubRequest request);

    @Operation(summary = "허브 단건 조회", description = "인증이 필요합니다. 삭제되었거나 존재하지 않는 허브는 HUB_001로 응답합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "허브 없음", content = @Content)
    })
    @GetMapping("/{hubId}")
    ResponseEntity<ApiResponse<HubResponseDto>> getOne(@PathVariable UUID hubId);

    @Operation(summary = "허브 목록 조회", description = "인증이 필요합니다. 삭제되지 않은 허브만 생성일 내림차순으로 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping
    ResponseEntity<ApiResponse<List<HubResponseDto>>> getAll();

    @Operation(summary = "허브 수정", description = "MASTER 또는 HUB_MANAGER 권한이 필요합니다. 하나 이상의 수정 항목을 보내야 하며, 전달한 필드만 변경됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "MASTER 또는 HUB_MANAGER 권한 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "허브 없음")
    })
    @PatchMapping("/{hubId}")
    ResponseEntity<ApiResponse<HubResponseDto>> update(
            @PathVariable UUID hubId, @Valid @RequestBody UpdateHubRequest request);

    @Operation(summary = "허브 삭제", description = "MASTER 권한이 필요합니다. 성공하면 data와 error가 모두 null인 응답을 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "MASTER 권한 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "허브 없음")
    })
    @DeleteMapping("/{hubId}")
    ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID hubId, @AuthenticationPrincipal CustomUserDetails userDetails);
}
