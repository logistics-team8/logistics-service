package com.logistics.userservice.presentation;

import com.logistics.common.response.ApiResponse;
import com.logistics.common.security.principal.CustomUserDetails;
import com.logistics.userservice.application.UserService;
import com.logistics.userservice.presentation.dto.user.SignUpRequest;
import com.logistics.userservice.presentation.dto.user.UpdateRequest;
import com.logistics.userservice.presentation.dto.user.UserInfoResponse;
import com.logistics.userservice.presentation.swagger.UserApi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController implements UserApi {
    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> signUp(@Valid @RequestBody SignUpRequest request) {
        userService.createUser(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body((ApiResponse.success(null)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserInfoResponse>> getMyInfo(
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        UserInfoResponse.from(userService.getUserInfo(customUserDetails.getId()))));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<Void>> updateMyInfo(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody UpdateRequest request) {
        userService.updateUser(request.toCommand(customUserDetails.getId()));
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteMyAccount(
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        userService.deleteUser(customUserDetails.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
