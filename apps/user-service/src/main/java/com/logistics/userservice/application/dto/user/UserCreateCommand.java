package com.logistics.userservice.application.dto.user;

import com.logistics.common.exception.BusinessException;
import com.logistics.userservice.domain.RequestedRole;
import com.logistics.userservice.error.AuthErrorCode;
import java.util.UUID;

public record UserCreateCommand(
        String username,
        String password,
        String name,
        String slackId,
        UUID hubId,
        UUID companyId,
        RequestedRole requestedRole) {
    public UserCreateCommand {
        if (requestedRole == RequestedRole.MASTER) {
            throw new BusinessException(AuthErrorCode.MASTER_ROLE_NOT_ALLOWED);
        }

        if (requestedRole == RequestedRole.COMPANY_MANAGER) {
            if (companyId == null) {
                throw new BusinessException(AuthErrorCode.COMPANY_ID_REQUIRED);
            }
            hubId = null;
        } else {
            if (hubId == null) {
                throw new BusinessException(AuthErrorCode.HUB_ID_REQUIRED);
            }
            companyId = null;
        }
    }
}
