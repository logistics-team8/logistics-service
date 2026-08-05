package com.logistics.userservice.presentation.dto.request;

import com.logistics.userservice.application.dto.UserSignUpCommand;
import com.logistics.userservice.domain.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UserSignUpRequest(
        @NotBlank
                @Size(min = 4, max = 10)
                @Pattern(regexp = "^[a-z0-9]+$")
                @Schema(description = "아이디", example = "test1234")
                String username,
        @NotBlank
                @Size(min = 8, max = 15, message = "비밀번호는 최소 8자 이상, 최대 15자 이하로 입력해야 합니다.")
                @Pattern(
                        regexp =
                                "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?`~]).+$",
                        message = "비밀번호는 소문자, 대문자, 숫자, 특수문자를 모두 포함해야 합니다.")
                @Schema(description = "패스워드", example = "password")
                String password,
        @NotBlank
                @Size(min = 1, max = 50, message = "이름은 50자 이하로 입력해 주세요.")
                @Schema(description = "회원 이름", example = "김철수")
                String name,
        @NotBlank
                @Pattern(
                        regexp = "^[UW][A-Z0-9]{8,12}$",
                        message = "올바른 Slack Member ID 형식이 아닙니다. (예: U1234567890)")
                @Schema(description = "Slack Member ID", example = "U06ABC12345")
                String slackId,
        @NotNull(message = "허브 ID는 필수 입력값입니다.")
                @Schema(
                        description = "Hub_id(PK)",
                        example = "7974488d-e80a-4de9-ac69-5746330eedd6")
                UUID hub_id,
        @Schema(description = "Company_id(PK)", example = "09e38839-6573-4de1-848c-77dda78f1926")
                UUID company_id,
        @NotNull(message = "권한은 필수 입력값입니다.")
                @Schema(description = "회원 권한", example = "COMPANY_MANAGER")
                Role role) {

    public UserSignUpCommand toCommand() {
        return new UserSignUpCommand(username, password, name, slackId, hub_id, company_id, role);
    }
}
