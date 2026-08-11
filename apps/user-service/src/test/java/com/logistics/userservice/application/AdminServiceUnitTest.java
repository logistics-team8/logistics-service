package com.logistics.userservice.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.logistics.common.exception.BusinessException;
import com.logistics.userservice.application.dto.admin.AdminApprovalCommand;
import com.logistics.userservice.application.dto.admin.AdminRejectCommand;
import com.logistics.userservice.application.port.AdminUserQueryRepository;
import com.logistics.userservice.application.validator.UserValidator;
import com.logistics.userservice.domain.Role;
import com.logistics.userservice.domain.User;
import com.logistics.userservice.domain.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

@DisplayName("AdminService - 단위 테스트")
@ExtendWith(MockitoExtension.class)
class AdminServiceUnitTest {
    @Mock private UserValidator validator;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AdminUserQueryRepository adminUserQueryRepository;
    @Mock private ApplicationEventPublisher applicationEventPublisher;
    @InjectMocks private AdminService adminService;

    // CRUD는 UserService와 로직이 거의 동일하기에 통합 테스트에서 성공 케이스만 작성

    // ============================== Approval ==============================
    @Test
    @DisplayName("허브 관리자는 다른 허브 회원 승인 불가.")
    void approveUser_fail_when_hub_manager_manages_different_hub() {
        // given
        UUID adminId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        AdminApprovalCommand command =
                new AdminApprovalCommand(adminId, Role.HUB_MANAGER, hubId, userId);

        User user = mock(User.class);

        given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(user));
        given(user.isManagedByHub(hubId)).willReturn(false);

        // when & then
        assertThrows(BusinessException.class, () -> adminService.approveUser(command));

        verify(user).isManagedByHub(hubId);
        verify(user, never()).approve(any());
    }

    @Test
    @DisplayName("허브 관리자는 다른 허브 회원 승인 거절 불가.")
    void rejectUser_fail_when_hub_manager_manages_different_hub() {
        // given
        UUID adminId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        AdminRejectCommand command =
                new AdminRejectCommand(adminId, Role.HUB_MANAGER, hubId, userId, "거절사유");

        User user = mock(User.class);

        given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(user));
        given(user.isManagedByHub(hubId)).willReturn(false);

        // when & then
        assertThrows(BusinessException.class, () -> adminService.rejectUser(command));

        verify(user).isManagedByHub(hubId);
        verify(user, never()).reject(any(), any());
    }
}
