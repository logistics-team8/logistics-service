package com.logistics.userservice.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.userservice.application.dto.UserSignUpCommand;
import com.logistics.userservice.application.token.TokenResult;
import com.logistics.userservice.config.test.AbstractIntegrationTest;
import com.logistics.userservice.domain.Role;
import com.logistics.userservice.domain.User;
import com.logistics.userservice.domain.UserRepository;
import com.logistics.userservice.presentation.dto.auth.LoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@SpringBootTest
@DisplayName("AuthService 통합 테스트")
class AuthServiceIntegrationTest extends AbstractIntegrationTest {
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AuthService authService;

    @BeforeEach
    void setUp() {
        UserSignUpCommand command =
                new UserSignUpCommand(
                        "test1234",
                        passwordEncoder.encode("Testtest123!"),
                        "김철수",
                        "U123456789",
                        null,
                        null,
                        Role.COMPANY_MANAGER);
        userRepository.save(User.create(command));
    }

    @Test
    @DisplayName("로그인에 성공하면 액세스 토큰과 리프레시 토큰을 발급한다.")
    void login_success() {
        // given
        LoginRequest loginRequest = new LoginRequest("test1234", "Testtest123!");

        // when
        TokenResult tokens = authService.login(loginRequest.toCommand());

        // then
        assertThat(tokens.accessToken()).isNotBlank();
        assertThat(tokens.refreshToken()).isNotBlank();
    }

    // TODO : AuthService 통합 테스트 추후에 추가 작성, 로그아웃, 토큰 재발급 성공 케이스
}
