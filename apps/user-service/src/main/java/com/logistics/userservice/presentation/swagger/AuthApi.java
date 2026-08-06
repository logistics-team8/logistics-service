package com.logistics.userservice.presentation.swagger;

import com.logistics.common.response.ApiResponse;
import com.logistics.userservice.presentation.dto.auth.LoginRequest;
import com.logistics.userservice.presentation.dto.auth.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "User / AUTH")
public interface AuthApi {
    @Operation(summary = "로그인", description = "사용자가 로그인 합니다.")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @Valid @RequestBody LoginRequest request);
}
