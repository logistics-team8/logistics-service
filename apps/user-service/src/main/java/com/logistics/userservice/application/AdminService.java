package com.logistics.userservice.application;

import com.logistics.common.error.CommonErrorCode;
import com.logistics.common.exception.BusinessException;
import com.logistics.userservice.application.dto.AdminApprovalCommand;
import com.logistics.userservice.application.dto.AdminRejectCommand;
import com.logistics.userservice.domain.Role;
import com.logistics.userservice.domain.User;
import com.logistics.userservice.domain.UserRepository;
import com.logistics.userservice.error.UserErrorCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {
    private final UserRepository userRepository;

    // ============================== Approval ==============================
    @Transactional
    public void approvalUser(AdminApprovalCommand command) {
        User user = findUserById(command.userId());
        validateManagerPermission(command.hubId(), command.role(), user);

        user.approve(command.userId());
    }

    @Transactional
    public void rejectUser(AdminRejectCommand command) {
        User user = findUserById(command.userId());
        validateManagerPermission(command.hubId(), command.role(), user);

        user.reject(command.userId(), command.reason());
    }

    //    getPendingUsers

    // ============================== Helper Method ====================================
    private User findUserById(UUID userId) {
        return userRepository
                .findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
    }

    private void validateManagerPermission(UUID adminHubId, Role adminRole, User user) {
        if (Role.HUB_MANAGER.equals(adminRole)) {
            if (!user.isManagedByHub(adminHubId)) {
                throw new BusinessException(CommonErrorCode.FORBIDDEN);
            }
        }
    }
}
