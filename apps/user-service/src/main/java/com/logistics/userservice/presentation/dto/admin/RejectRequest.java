package com.logistics.userservice.presentation.dto.admin;

import com.logistics.common.security.principal.CustomUserDetails;
import com.logistics.userservice.application.dto.admin.AdminRejectCommand;
import com.logistics.userservice.domain.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record RejectRequest(
        @Size(max = 255, message = "거절 사유가 너무 깁니다. 255자 이내로 작성해 주세요.")
                @Schema(description = "거절 사유", example = "거절 사유")
                String reason) {
    public AdminRejectCommand toCommand(CustomUserDetails principal, UUID userId) {
        return new AdminRejectCommand(
                principal.getId(),
                Role.valueOf(principal.getRole()),
                principal.getHubId(),
                userId,
                reason);
    }
}
