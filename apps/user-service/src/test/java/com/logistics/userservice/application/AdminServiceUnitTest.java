package com.logistics.userservice.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

import com.logistics.common.error.CommonErrorCode;
import com.logistics.common.exception.BusinessException;
import com.logistics.userservice.application.dto.admin.AdminApprovalCommand;
import com.logistics.userservice.application.dto.admin.AdminRejectCommand;
import com.logistics.userservice.application.validator.UserValidator;
import com.logistics.userservice.domain.*;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("AdminService - 단위 테스트")
@ExtendWith(MockitoExtension.class)
class AdminServiceUnitTest {
    @Mock private UserValidator validator;
    @Mock private UserRepository userRepository;
    @InjectMocks private AdminService adminService;

    private UUID adminId;
    private UUID userId;
    private UUID hubId;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        userId = UUID.randomUUID();
        hubId = UUID.randomUUID();
    }

    // CRUD는 UserService와 로직이 거의 동일하기에 중복 되는 부분 제외

    // ============================== Approval ==============================
    @Test
    @DisplayName("허브 관리자는 다른 허브 회원 승인 불가.")
    void approveUser_fail_when_hub_manager_manages_different_hub() {
        // given
        AdminApprovalCommand command =
                new AdminApprovalCommand(adminId, Role.HUB_MANAGER, hubId, userId);

        User user = mock(User.class);

        given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(user));

        willThrow(new BusinessException(CommonErrorCode.FORBIDDEN))
                .given(validator)
                .validateManagerPermission(command.hubId(), command.role(), user);

        // when & then
        assertThrows(BusinessException.class, () -> adminService.approveUser(command));

        verify(validator).validateManagerPermission(command.hubId(), command.role(), user);
        verify(user, never()).approve(any());
    }

    @Test
    @DisplayName("허브 관리자는 다른 허브 회원 승인 거절 불가.")
    void rejectUser_fail_when_hub_manager_manages_different_hub() {
        // given
        AdminRejectCommand command =
                new AdminRejectCommand(adminId, Role.HUB_MANAGER, hubId, userId, "거절사유");

        User user = mock(User.class);

        given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(user));

        willThrow(new BusinessException(CommonErrorCode.FORBIDDEN))
                .given(validator)
                .validateManagerPermission(command.hubId(), command.role(), user);

        // when & then
        assertThrows(BusinessException.class, () -> adminService.rejectUser(command));

        verify(validator).validateManagerPermission(command.hubId(), command.role(), user);
        verify(user, never()).reject(any(), any());
    }
}
