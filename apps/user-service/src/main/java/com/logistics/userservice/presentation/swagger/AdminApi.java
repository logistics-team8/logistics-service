package com.logistics.userservice.presentation.swagger;

import com.logistics.common.response.ApiResponse;
import com.logistics.common.response.PageResponse;
import com.logistics.common.security.principal.CustomUserDetails;
import com.logistics.userservice.application.dto.admin.UserApprovalInfo;
import com.logistics.userservice.presentation.dto.admin.AdminSearchRequest;
import com.logistics.userservice.presentation.dto.admin.AdminUserInfoResponse;
import com.logistics.userservice.presentation.dto.admin.RejectRequest;
import com.logistics.userservice.presentation.dto.user.UserCreateRequest;
import com.logistics.userservice.presentation.dto.user.UserUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin")
public interface AdminApi {
    // ============================== CRUD ==============================
    @Operation(summary = "회원 등록", description = "관리자가 신규 회원을 등록합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "201",
                description = "회원 등록 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "필수 입력값 누락 또는 유효성 체크 실패."),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "권한 부족."),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "아이디 또는 슬랙 아이디 중복."),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "서버 오류"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "503",
                description = "Service-Server 오류")
    })
    public ResponseEntity<ApiResponse<Void>> createUser(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody UserCreateRequest signUpRequest);

    @Operation(summary = "회원 목록 검색", description = "관리자가 회원 목록을 조회합니다.")
    public ApiResponse<PageResponse<AdminUserInfoResponse>> searchUsers(
            @AuthenticationPrincipal CustomUserDetails principal,
            @ParameterObject @Valid @ModelAttribute AdminSearchRequest request,
            @ParameterObject @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable);

    @Operation(summary = "회원 조회", description = "관리자가 회원을 조회합니다.")
    public ApiResponse<AdminUserInfoResponse> getUserById(@PathVariable UUID userId);

    @Operation(summary = "회원 정보 수정", description = "관리자가 회원 정보를 수정합니다.")
    public ApiResponse<Void> updateUser(
            @PathVariable UUID userId, @RequestBody @Valid UserUpdateRequest request);

    @Operation(summary = "회원 삭제", description = "관리자가 회원을 삭제합니다.")
    public ApiResponse<Void> deleteUser(@PathVariable UUID userId);

    // ============================== Approval ==============================
    @Operation(summary = "회원가입 요청 승인", description = "관리자가 회원가입 요청을 승인합니다.")
    public ApiResponse<Void> approveUser(
            @AuthenticationPrincipal CustomUserDetails principal, @PathVariable UUID userId);

    @Operation(summary = "회원가입 요청 거절", description = "관리자가 회원가입 요청을 거절합니다.")
    public ApiResponse<Void> rejectUser(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID userId,
            @Valid @RequestBody RejectRequest request);

    @Operation(summary = "회원가입 요청 리스트 검색", description = "관리자가 회원가입 요청 목록을 조회합니다.")
    public ApiResponse<PageResponse<UserApprovalInfo>> getPendingUsers(
            @AuthenticationPrincipal CustomUserDetails principal,
            @ParameterObject @Valid @ModelAttribute AdminSearchRequest request,
            @ParameterObject @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable);
}
