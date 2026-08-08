package com.logistics.userservice.presentation;

import com.logistics.common.response.ApiResponse;
import com.logistics.common.security.principal.CustomUserDetails;
import com.logistics.userservice.application.AdminService;
import com.logistics.userservice.application.dto.AdminApprovalCommand;
import com.logistics.userservice.presentation.dto.admin.RejectRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/users")
public class AdminController {
    private final AdminService adminService;

    // ============================== CRUD ==============================
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createUser() {
        throw new UnsupportedOperationException("개발 중 입니다.");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Void>> searchUsers() {
        throw new UnsupportedOperationException("개발 중 입니다.");
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> getUser(@PathVariable String userId) {
        throw new UnsupportedOperationException("개발 중 입니다.");
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> updateUser(@PathVariable String userId) {
        throw new UnsupportedOperationException("개발 중 입니다.");
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable String userId) {
        throw new UnsupportedOperationException("개발 중 입니다.");
    }

    // ============================== Approval ==============================
    @PatchMapping("/{userId}/approve")
    public ResponseEntity<ApiResponse<Void>> approvalUser(
            @AuthenticationPrincipal CustomUserDetails principal, @PathVariable UUID userId) {
        adminService.approvalUser(AdminApprovalCommand.of(principal, userId));
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PatchMapping("/{userId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectUser(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID userId,
            @Valid @RequestBody RejectRequest request) {
        adminService.rejectUser(request.toCommand(principal, userId));
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/pending-approvals") // RequestBody
    public ResponseEntity<ApiResponse<Void>> getPendingUsers(@PathVariable String userId) {
        throw new UnsupportedOperationException("개발 중 입니다.");
    }
}
