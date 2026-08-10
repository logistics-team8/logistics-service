package com.logistics.userservice.application;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.logistics.common.exception.BusinessException;
import com.logistics.userservice.application.dto.user.UserCreateCommand;
import com.logistics.userservice.application.dto.user.UserUpdateCommand;
import com.logistics.userservice.application.validator.UserValidator;
import com.logistics.userservice.domain.Role;
import com.logistics.userservice.domain.UserRepository;
import com.logistics.userservice.error.UserErrorCode;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@DisplayName("UserService - 단위 테스트")
@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {
    @Mock private UserRepository userRepository;
    @Mock private UserValidator userValidator;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private UserService userService;

    @Nested
    @DisplayName("회원가입 실패 테스트")
    class createUser {
        @Test
        @DisplayName("이미 존재하는 아이디일 시 USER_DUPLICATE_USERNAME 예외가 발생해야한다.")
        void createMember_fail_when_username_is_duplicate() {
            // given
            UserCreateCommand command =
                    new UserCreateCommand(
                            "test1234",
                            "Testtest123!",
                            "김철수",
                            "U123456789",
                            UUID.randomUUID(),
                            UUID.randomUUID(),
                            Role.COMPANY_MANAGER);

            doThrow(new BusinessException(UserErrorCode.USER_DUPLICATE_USERNAME))
                    .when(userValidator)
                    .validateDuplicate(command);

            // when & then
            assertThatThrownBy(() -> userService.createUser(command))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(UserErrorCode.USER_DUPLICATE_USERNAME.message());

            verify(userValidator).validateDuplicate(command);
            verify(userRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("이미 존재하는 Slack 아이디일 시 USER_DUPLICATE_SLACK_ID 예외가 발생해야한다.")
        void createMember_fail_when_slack_id_is_duplicate() {
            // given
            UserCreateCommand command =
                    new UserCreateCommand(
                            "test1234",
                            "Testtest123!",
                            "김철수",
                            "U123456789",
                            UUID.randomUUID(),
                            UUID.randomUUID(),
                            Role.COMPANY_MANAGER);

            doThrow(new BusinessException(UserErrorCode.USER_DUPLICATE_SLACK_ID))
                    .when(userValidator)
                    .validateDuplicate(command);

            // when & then
            assertThatThrownBy(() -> userService.createUser(command))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(UserErrorCode.USER_DUPLICATE_SLACK_ID.message());

            verify(userValidator).validateDuplicate(command);
            verify(userRepository, never()).saveAndFlush(any());
        }
    }

    @Test
    @DisplayName("유저 조회 시 회원이 존재하지 않는 경우 USER_NOT_FOUND 예외가 발생해야한다.")
    void getUser_fail_when_user_not_found() {
        // given
        UUID userId = UUID.randomUUID();

        given(userRepository.findByIdAndDeletedAtIsNull(eq(userId))).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getUserInfo(userId))
                .isInstanceOf(BusinessException.class)
                .hasMessage(UserErrorCode.USER_NOT_FOUND.message());

        verify(userRepository).findByIdAndDeletedAtIsNull(userId);
    }

    @Test
    @DisplayName("회운 수정 시 회원이 존재하지 않는 경우 USER_NOT_FOUND 예외가 발생해야한다.")
    void updateUser_fail_when_user_not_found() {
        // given
        UUID userId = UUID.randomUUID();
        UserUpdateCommand command = new UserUpdateCommand(userId, "test1234", "U12345678");

        given(userRepository.findByIdAndDeletedAtIsNull(any())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.updateUser(command))
                .isInstanceOf(BusinessException.class)
                .hasMessage(UserErrorCode.USER_NOT_FOUND.message());

        verify(userRepository).findByIdAndDeletedAtIsNull(command.userId());
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("회원 탈퇴 시 회원 존재하지 않는 경우 USER_NOT_FOUND 예외가 발생해야한다.")
    void deleteUser_fail_when_user_not_found() {
        // given
        UUID userId = UUID.randomUUID();

        given(userRepository.findByIdAndDeletedAtIsNull(any())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.deleteUser(userId))
                .isInstanceOf(BusinessException.class)
                .hasMessage(UserErrorCode.USER_NOT_FOUND.message());
    }
}
