package com.logistics.userservice.presentation.dto.user;

import com.logistics.userservice.application.dto.user.UserCreateCommand;
import com.logistics.userservice.domain.RequestedRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UserCreateRequest(
        @NotBlank(message = "아이디를 입력해주세요.")
                @Size(min = 4, max = 10, message = "아이디는 4자 이상 10자 이하로 입력해야 합니다.")
                @Pattern(regexp = "^[a-z0-9]+$", message = "아이디는 영문 소문자와 숫자만 사용 가능합니다.")
                @Schema(description = "아이디", example = "test1234")
                String username,
        @NotBlank(message = "비밀번호를 입력해주세요.")
                @Size(min = 8, max = 15, message = "비밀번호는 최소 8자 이상, 최대 15자 이하로 입력해야 합니다.")
                @Pattern(
                        regexp =
                                "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?`~]).+$",
                        message = "비밀번호는 소문자, 대문자, 숫자, 특수문자를 모두 포함해야 합니다.")
                @Schema(description = "비밀번호", example = "Test1234!")
                String password,
        @NotBlank(message = "이름을 입력해주세요.")
                @Size(min = 1, max = 50, message = "이름은 50자 이하로 입력해 주세요.")
                @Schema(description = "회원 이름", example = "김철수")
                String name,
        @NotBlank(message = "Slack Member ID를 입력해주세요.")
                @Pattern(
                        regexp = "^[UW][A-Z0-9]{8,12}$",
                        message = "올바른 Slack Member ID 형식이 아닙니다. (예: U1234567890)")
                @Schema(description = "Slack Member ID", example = "U06ABC12345")
                String slackId,
        @Schema(description = "Hub_id(PK)", example = "7974488d-e80a-4de9-ac69-5746330eedd6")
                UUID hubId,
        @Schema(description = "Company_id(PK) COMPANY_MANAGER일 때 입력(업체 배송 관리자는 X)") UUID companyId,
        @NotNull(message = "요청 권한을 입력해주세요.")
                @Schema(description = "요청 회원 권한", example = "COMPANY_MANAGER")
                RequestedRole requestedRole) {

    public UserCreateCommand toCommand() {
        return new UserCreateCommand(
                username, password, name, slackId, hubId, companyId, requestedRole);
    }
}
