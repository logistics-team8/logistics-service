package com.logistics.userservice.application;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

import com.logistics.common.error.CommonErrorCode;
import com.logistics.common.exception.BusinessException;
import com.logistics.userservice.application.dto.user.UserCreateCommand;
import com.logistics.userservice.application.token.TokenPayload;
import com.logistics.userservice.application.token.TokenProvider;
import com.logistics.userservice.domain.RequestedRole;
import com.logistics.userservice.domain.User;
import com.logistics.userservice.domain.UserRepository;
import com.logistics.userservice.domain.redis.RefreshTokenRepository;
import com.logistics.userservice.domain.redis.RoleCacheRepository;
import com.logistics.userservice.error.AuthErrorCode;
import com.logistics.userservice.error.UserErrorCode;
import com.logistics.userservice.presentation.dto.auth.LoginRequest;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;

@DisplayName("AuthService - 단위 테스트")
@ExtendWith(MockitoExtension.class)
class AuthServiceUnitTest {
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TokenProvider tokenProvider;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private RoleCacheRepository roleCacheRepository;
    @InjectMocks private AuthService authService;

    @Nested
    @DisplayName("로그인 실패 테스트")
    class Login {
        @Test
        @DisplayName("로그인 시 존재하지 않는 아이디를 입력하면 INVALID_LOGIN 예외가 발생해야한다.")
        void login_fail_when_invalid_login() {
            // given
            LoginRequest request = new LoginRequest("test1234", "testtest1234!");

            given(userRepository.findByUsernameAndDeletedAtIsNull(request.username()))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.login(request.toCommand()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(AuthErrorCode.INVALID_LOGIN.message());

            verify(userRepository).findByUsernameAndDeletedAtIsNull(request.username());
            verify(passwordEncoder, never()).matches(anyString(), anyString());
        }

        @Test
        @DisplayName("로그인 시 틀린 비밀번호를 입력하면 INVALID_LOGIN 예외가 발생해야한다.")
        void login_fail_when_invalid_password() {
            // given
            LoginRequest request = new LoginRequest("test1234", "testtest1234!");

            UserCreateCommand command =
                    new UserCreateCommand(
                            "test1234",
                            "비밀번호",
                            "김철수",
                            "U123456789",
                            null,
                            null,
                            RequestedRole.COMPANY_MANAGER);

            User mockUser = User.create(command);

            given(userRepository.findByUsernameAndDeletedAtIsNull(request.username()))
                    .willReturn(Optional.of(mockUser));

            given(passwordEncoder.matches(request.password(), mockUser.getPassword()))
                    .willReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.login(request.toCommand()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(AuthErrorCode.INVALID_LOGIN.message());

            verify(userRepository).findByUsernameAndDeletedAtIsNull(request.username());
            verify(passwordEncoder).matches(request.password(), mockUser.getPassword());
        }

        @Test
        @DisplayName("리프레시 토큰 저장에 실패하면 INTERNAL_SERVER_ERROR 예외가 발생한다.")
        void login_fail_when_refreshToken_save_failed() {
            // given
            LoginRequest request = new LoginRequest("test1234", "testtest1234!");

            UserCreateCommand command =
                    new UserCreateCommand(
                            "test1234",
                            "비밀번호",
                            "김철수",
                            "U123456789",
                            null,
                            null,
                            RequestedRole.COMPANY_MANAGER);

            User mockUser = User.create(command);
            mockUser.approve(UUID.randomUUID());

            given(userRepository.findByUsernameAndDeletedAtIsNull(request.username()))
                    .willReturn(Optional.of(mockUser));

            given(passwordEncoder.matches(request.password(), mockUser.getPassword()))
                    .willReturn(true);

            willThrow(new DataAccessException("Redis Error") {})
                    .given(refreshTokenRepository)
                    .save(any(), any());

            // when & then
            assertThatThrownBy(() -> authService.login(request.toCommand()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(CommonErrorCode.INTERNAL_SERVER_ERROR.message());
        }

        @Test
        @DisplayName("Role 캐싱에 실패해도 로그인이 성공한다.")
        void login_success_when_roleCacheRepository_save_failed() {
            // given
            LoginRequest request = new LoginRequest("test1234", "testtest1234!");

            UserCreateCommand command =
                    new UserCreateCommand(
                            "test1234",
                            "비밀번호",
                            "김철수",
                            "U123456789",
                            null,
                            null,
                            RequestedRole.COMPANY_MANAGER);

            User mockUser = User.create(command);
            mockUser.approve(UUID.randomUUID());

            given(userRepository.findByUsernameAndDeletedAtIsNull(request.username()))
                    .willReturn(Optional.of(mockUser));

            given(passwordEncoder.matches(request.password(), mockUser.getPassword()))
                    .willReturn(true);

            willThrow(new DataAccessException("Redis Error") {})
                    .given(roleCacheRepository)
                    .save(any(), any());

            // when & then
            assertThatCode(() -> authService.login(request.toCommand())).doesNotThrowAnyException();

            verify(refreshTokenRepository).save(any(), any());
            verify(roleCacheRepository).save(any(), any());
        }
    }

    @Nested
    @DisplayName("로그아웃 실패 테스트")
    class Logout {
        @Test
        @DisplayName("로그아웃 시 만료된 액세스 토큰을 보내도 성공한다.")
        void logout_success_when_token_expired() {
            // given
            String accessToken = "accessToken";
            UUID userId = UUID.randomUUID();

            Claims claims = mock(Claims.class);
            given(claims.getSubject()).willReturn(userId.toString());

            given(tokenProvider.getAllClaimsFromAccessToken(accessToken))
                    .willThrow(new ExpiredJwtException(null, claims, "토큰 만료"));

            // when & then
            assertThatCode(() -> authService.logout(accessToken)).doesNotThrowAnyException();

            verify(refreshTokenRepository).delete(userId);
            verify(roleCacheRepository).delete(userId);
        }

        @Test
        @DisplayName("로그아웃 시 유효하지 않은 액세스 토큰을 보내면 TOKEN_INVALID 예외가 발생한다.")
        void logout_fail_when_invalid_token() {
            // given
            String accessToken = "accessToken";

            given(tokenProvider.getAllClaimsFromAccessToken(accessToken))
                    .willThrow(new JwtException("토큰 유효하지 않음"));

            // when & then
            assertThatThrownBy(() -> authService.logout(accessToken))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(AuthErrorCode.TOKEN_INVALID.message());
        }

        // TODO : 추후 Redis 관련 로그아웃 추가 테스트 진행

    }

    @Nested
    @DisplayName("토큰 재발급 실패 테스트")
    class Reissue {
        @Test
        @DisplayName("Redis에 저장된 토큰과 쿠키로 보낸 토큰이 일치하지않으면 TOKEN_INVALID 예외가 발생한다.")
        void reissue_fail_when_token_mismatch() {
            // given
            String refreshToken = "refreshToken";
            String redisToken = "redisToken";
            UUID userId = UUID.randomUUID();
            TokenPayload tokenPayload = mock(TokenPayload.class);

            given(tokenProvider.getAllClaimsFromRefreshToken(refreshToken))
                    .willReturn(tokenPayload);

            given(tokenPayload.userId()).willReturn(userId);

            given(refreshTokenRepository.findByUserId(userId)).willReturn(Optional.of(redisToken));

            // when & then
            assertThatThrownBy(() -> authService.reissue(refreshToken))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(AuthErrorCode.TOKEN_INVALID.message());
        }

        @Test
        @DisplayName("토큰 재발급 시 만료된 리프레시 토큰을 보내면 TOKEN_EXPIRED 예외가 발생한다.")
        void reissue_fail_when_expired_token() {
            // given
            String refreshToken = "refreshToken";

            given(tokenProvider.getAllClaimsFromRefreshToken(refreshToken))
                    .willThrow(new ExpiredJwtException(null, null, "토큰 에러"));

            // when & then
            assertThatThrownBy(() -> authService.reissue(refreshToken))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(AuthErrorCode.TOKEN_EXPIRED.message());
        }

        @Test
        @DisplayName("토큰 재발급 시 유효하지 않은 리프레시 토큰을 보내면 TOKEN_INVALID 예외가 발생한다.")
        void reissue_fail_when_invalid_token() {
            // given
            String refreshToken = "refreshToken";

            given(tokenProvider.getAllClaimsFromRefreshToken(refreshToken))
                    .willThrow(new JwtException("토큰 에러"));

            // when & then
            assertThatThrownBy(() -> authService.reissue(refreshToken))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(AuthErrorCode.TOKEN_INVALID.message());
        }

        @Test
        @DisplayName("탈퇴하거나 존재하지 않는 회원이 토큰 재발급을 하면 USER_NOT_FOUND 예외가 발생한다.")
        void reissue_fail_when_user_not_found() {
            // given
            String refreshToken = "refreshToken";
            UUID userId = UUID.randomUUID();
            TokenPayload tokenPayload = mock(TokenPayload.class);

            given(tokenProvider.getAllClaimsFromRefreshToken(refreshToken))
                    .willReturn(tokenPayload);

            given(tokenPayload.userId()).willReturn(userId);

            given(refreshTokenRepository.findByUserId(userId))
                    .willReturn(Optional.of(refreshToken));

            given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.reissue(refreshToken))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(UserErrorCode.USER_NOT_FOUND.message());
        }
    }
}
