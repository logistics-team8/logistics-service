package com.logistics.userservice.presentation;

import com.logistics.common.response.ApiResponse;
import com.logistics.userservice.application.AdminService;
import com.logistics.userservice.presentation.dto.admin.RejectRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/users")
public class AdminController {
    private final AdminService adminService;

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

    @PatchMapping("/{userId}/approve")
    public ResponseEntity<ApiResponse<Void>> approvalUser(@PathVariable String userId) {
        adminService.approvalUser(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PatchMapping("/{userId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectUser(
            @PathVariable UUID userId, @Valid @RequestBody RejectRequest request) {
        adminService.rejectUser(request.toCommand(userId));
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/pending-approvals") // RequestBody
    public ResponseEntity<ApiResponse<Void>> getPendingUsers(@PathVariable String userId) {
        throw new UnsupportedOperationException("개발 중 입니다.");
    }
}
