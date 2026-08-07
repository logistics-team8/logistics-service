package com.logistics.userservice.presentation.dto.user;

import com.logistics.userservice.application.dto.UserUpdateCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateRequest(
        @NotBlank
        @Size(min = 1, max = 50, message = "이름은 50자 이하로 입력해 주세요.")
        @Schema(description = "회원 이름", example = "김철수")
        String name,

        @NotBlank
        @Pattern(
                regexp = "^[UW][A-Z0-9]{8,12}$",
                message = "올바른 Slack Member ID 형식이 아닙니다. (예: U1234567890)")
        @Schema(description = "Slack Member ID", example = "U06ABC12345")
        String slackId) {
    public UserUpdateCommand toCommand(UUID userId) {
        return new UserUpdateCommand(userId, name, slackId);
    }
}
