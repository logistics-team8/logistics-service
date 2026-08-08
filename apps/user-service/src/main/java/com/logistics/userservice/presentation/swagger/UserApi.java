package com.logistics.userservice.presentation.swagger;

import com.logistics.common.response.ApiResponse;
import com.logistics.userservice.presentation.dto.user.SignUpRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "User / AUTH")
public interface UserApi {
    @Operation(summary = "회원가입", description = "사용자가 신규 회원을 등록합니다.")
    public ResponseEntity<ApiResponse<Void>> signUp(@Valid @RequestBody SignUpRequest request);
}
