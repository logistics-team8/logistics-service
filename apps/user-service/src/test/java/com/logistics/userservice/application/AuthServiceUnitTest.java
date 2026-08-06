package com.logistics.userservice.application;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.logistics.common.exception.BusinessException;
import com.logistics.userservice.application.dto.UserSignUpCommand;
import com.logistics.userservice.domain.Role;
import com.logistics.userservice.domain.User;
import com.logistics.userservice.domain.UserRepository;
import com.logistics.userservice.presentation.dto.request.LoginRequest;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceUnitTest {
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private AuthService authService;

    @Nested
    @DisplayName("로그인 실패 테스트 - Service")
    class Login {
        @Test
        @DisplayName("로그인 시 존재하지 않는 아이디를 입력하면 예외가 발생해야한다.")
        void login_fail_when_invalid_login() {
            // given
            LoginRequest request = new LoginRequest("test1234", "testtest1234!");

            given(userRepository.findByUsernameAndDeletedAtIsNull(request.username()))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("아이디가 존재하지 않거나 비밀번호가 올바르지 않습니다.");

            verify(userRepository).findByUsernameAndDeletedAtIsNull(request.username());
            verify(passwordEncoder, never()).matches(anyString(), anyString());
        }

        @Test
        @DisplayName("로그인 시 틀린 비밀번호를 입력하면 예외가 발생해야한다.")
        void login_fail_when_invalid_password() {
            // given
            LoginRequest request = new LoginRequest("test1234", "testtest1234!");

            UserSignUpCommand command =
                    new UserSignUpCommand(
                            "test1234",
                            "비밀번호",
                            "김철수",
                            "U123456789",
                            null,
                            null,
                            Role.COMPANY_MANAGER);

            User mockUser = User.create(command);

            given(userRepository.findByUsernameAndDeletedAtIsNull(request.username()))
                    .willReturn(Optional.of(mockUser));

            given(passwordEncoder.matches(request.password(), mockUser.getPassword()))
                    .willReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("아이디가 존재하지 않거나 비밀번호가 올바르지 않습니다.");

            verify(userRepository).findByUsernameAndDeletedAtIsNull(request.username());
            verify(passwordEncoder).matches(request.password(), mockUser.getPassword());
        }
    }
}
