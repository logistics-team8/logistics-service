package com.logistics.userservice.presentation;

import com.logistics.common.response.ApiResponse;
import com.logistics.common.response.PageResponse;
import com.logistics.common.response.PageableUtil;
import com.logistics.common.security.principal.CustomUserDetails;
import com.logistics.userservice.application.AdminService;
import com.logistics.userservice.application.dto.admin.AdminApprovalCommand;
import com.logistics.userservice.application.dto.admin.AdminUserInfo;
import com.logistics.userservice.application.dto.admin.UserApprovalInfo;
import com.logistics.userservice.application.dto.user.UserContext;
import com.logistics.userservice.presentation.dto.admin.AdminApproveSearchRequest;
import com.logistics.userservice.presentation.dto.admin.AdminSearchRequest;
import com.logistics.userservice.presentation.dto.admin.AdminUserInfoResponse;
import com.logistics.userservice.presentation.dto.admin.RejectRequest;
import com.logistics.userservice.presentation.dto.user.UserCreateRequest;
import com.logistics.userservice.presentation.dto.user.UserUpdateRequest;
import com.logistics.userservice.presentation.swagger.AdminApi;
import jakarta.validation.Valid;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/users")
public class AdminController implements AdminApi {
    private static final Set<String> ALLOWED_SORTS = Set.of("createdAt", "updatedAt");

    private final AdminService adminService;

    // ============================== CRUD ==============================
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createUser(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody UserCreateRequest signUpRequest) {
        adminService.createUserByAdmin(signUpRequest.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(null));
    }

    @GetMapping
    public ApiResponse<PageResponse<AdminUserInfoResponse>> searchUsers(
            @AuthenticationPrincipal CustomUserDetails principal,
            @ParameterObject @Valid @ModelAttribute AdminSearchRequest request,
            @ParameterObject @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        Pageable normalizedPageable = normalizeSort(PageableUtil.normalize(pageable));

        return ApiResponse.success(
                PageResponse.from(
                        adminService
                                .getUsersInfo(
                                        UserContext.from(principal),
                                        request.toQuery(),
                                        normalizedPageable)
                                .map(AdminUserInfoResponse::from)));
    }

    @GetMapping("/{userId}")
    public ApiResponse<AdminUserInfoResponse> getUserById(@PathVariable UUID userId) {
        AdminUserInfo adminUserInfo = adminService.getUserInfo(userId);
        return ApiResponse.success(AdminUserInfoResponse.from(adminUserInfo));
    }

    @PatchMapping("/{userId}")
    public ApiResponse<Void> updateUser(
            @PathVariable UUID userId, @RequestBody @Valid UserUpdateRequest request) {
        adminService.updateUser(request.toCommand(userId));
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<Void> deleteUser(@PathVariable UUID userId) {
        adminService.deleteUser(userId);
        return ApiResponse.success(null);
    }

    // ============================== Approval ==============================
    @PatchMapping("/{userId}/approve")
    public ApiResponse<Void> approveUser(
            @AuthenticationPrincipal CustomUserDetails principal, @PathVariable UUID userId) {
        adminService.approveUser(AdminApprovalCommand.of(principal, userId));
        return ApiResponse.success(null);
    }

    @PatchMapping("/{userId}/reject")
    public ApiResponse<Void> rejectUser(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID userId,
            @Valid @RequestBody RejectRequest request) {
        adminService.rejectUser(request.toCommand(principal, userId));
        return ApiResponse.success(null);
    }

    @GetMapping("/pending-approvals")
    public ApiResponse<PageResponse<UserApprovalInfo>> getPendingUsers(
            @AuthenticationPrincipal CustomUserDetails principal,
            @ParameterObject @Valid @ModelAttribute AdminApproveSearchRequest request,
            @ParameterObject @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        Pageable normalizedPageable = normalizeSort(PageableUtil.normalize(pageable));

        return ApiResponse.success(
                PageResponse.from(
                        adminService.getPendingUsers(
                                UserContext.from(principal),
                                request.toQuery(),
                                normalizedPageable)));
    }

    private Pageable normalizeSort(Pageable pageable) {
        boolean isValidSort =
                pageable.getSort().stream()
                        .allMatch(order -> ALLOWED_SORTS.contains(order.getProperty()));

        return isValidSort
                ? pageable
                : PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        Sort.Direction.DESC,
                        "createdAt");
    }
}
