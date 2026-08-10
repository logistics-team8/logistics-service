package com.logistics.userservice.presentation.dto.auth;

import com.logistics.userservice.application.dto.auth.UserLoginCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "아이디를 입력해주세요.") @Schema(description = "아이디", example = "test1234")
                String username,
        @NotBlank(message = "비밀번호를 입력해주세요.") @Schema(description = "비밀번호", example = "Test1234!")
                String password) {
    public UserLoginCommand toCommand() {
        return new UserLoginCommand(username, password);
    }
}
