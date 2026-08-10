package com.logistics.userservice.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.logistics.userservice.application.AuthService;
import com.logistics.userservice.application.dto.auth.UserLoginCommand;
import com.logistics.userservice.application.token.TokenResult;
import com.logistics.userservice.config.test.AbstractControllerTest;
import com.logistics.userservice.error.AuthErrorCode;
import com.logistics.userservice.presentation.cookie.CookieProvider;
import com.logistics.userservice.presentation.dto.auth.LoginRequest;
import jakarta.servlet.http.Cookie;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DisplayName("AuthController - 단위 테스트")
@WebMvcTest(controllers = AuthController.class)
class AuthControllerUnitTest extends AbstractControllerTest {
    @MockitoBean private AuthService authService;
    @MockitoBean private CookieProvider cookieProvider;

    @Nested
    @DisplayName("로그인 테스트")
    class Login {
        @Test
        @DisplayName("로그인 성공")
        void login_success() throws Exception {
            // given
            LoginRequest loginRequest = new LoginRequest("test1234", "Testtest123");
            String refreshToken = "refreshToken";
            TokenResult tokenResult = new TokenResult("accessToken", refreshToken);

            ResponseCookie cookie =
                    ResponseCookie.from("refreshToken", refreshToken)
                            .maxAge(0)
                            .path("/")
                            .sameSite("Strict")
                            .httpOnly(true)
                            .build();

            given(authService.login(any(UserLoginCommand.class))).willReturn(tokenResult);
            given(cookieProvider.createRefreshToken(refreshToken)).willReturn(cookie);

            // when & then
            mockMvc.perform(
                            post("/api/v1/auth/login")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(cookie().value("refreshToken", "refreshToken"))
                    .andExpect(cookie().httpOnly("refreshToken", true))
                    .andExpect(cookie().secure("refreshToken", false))
                    .andExpect(jsonPath("$.data.accessToken").value("accessToken"));

            verify(authService).login(any(UserLoginCommand.class));
        }

        @ParameterizedTest
        @MethodSource("loginTestCase")
        @DisplayName("로그인 시 유효성 체크를 통과하지 못하면 예외가 발생해야한다.")
        void login_fail_when_invalid(LoginRequest request) throws Exception {
            // when & then
            mockMvc.perform(
                            post("/api/v1/auth/login")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(authService);
        }

        static Stream<LoginRequest> loginTestCase() {
            return Stream.of(
                    new LoginRequest("", "Testtest123!"), new LoginRequest("test1234", ""));
        }
    }

    @Nested
    @WithMockUser
    @DisplayName("로그아웃")
    class Logout {
        @Test
        @DisplayName("Logout에 성공하면 RefreshToken이 담긴 Cookie를 삭제한다.")
        void login_success() throws Exception {
            // given
            String accessToken = "accessToken";

            ResponseCookie refreshToken =
                    ResponseCookie.from("refreshToken", "").maxAge(0).path("/").build();

            given(cookieProvider.deleteRefreshToken()).willReturn(refreshToken);

            // when & then
            mockMvc.perform(
                            post("/api/v1/auth/logout")
                                    .header("Authorization", "Bearer " + accessToken)
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(cookie().exists("refreshToken"))
                    .andExpect(cookie().value("refreshToken", ""))
                    .andExpect(cookie().maxAge("refreshToken", 0));

            verify(authService).logout(accessToken);
        }

        @Test
        @DisplayName("AccessToken이 Null일 시 401 예외가 발생한다. ")
        void login_fail_when_token_invalid() throws Exception {
            // given
            String accessToken = "";

            // when & then
            mockMvc.perform(
                            post("/api/v1/auth/logout")
                                    .header("Authorization", "Bearer " + accessToken)
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(
                            jsonPath("$.error.message")
                                    .value(AuthErrorCode.TOKEN_INVALID.message()));

            verify(authService, never()).logout(any());
        }
    }

    @Nested
    @DisplayName("토큰 재발급")
    class Reissue {
        @Test
        @DisplayName("토큰 재발급에 성공하면 Cookie를 생성하고 AccessToken을 반환한다.")
        void reissue_success() throws Exception {
            // given
            String refreshToken = "refreshToken";
            TokenResult tokenResult = new TokenResult("accessToken", "refreshToken");

            ResponseCookie cookie =
                    ResponseCookie.from("refreshToken", refreshToken)
                            .maxAge(0)
                            .path("/")
                            .sameSite("Strict")
                            .httpOnly(true)
                            .build();

            given(authService.reissue(any())).willReturn(tokenResult);
            given(cookieProvider.createRefreshToken(refreshToken)).willReturn(cookie);

            // when & then
            mockMvc.perform(
                            post("/api/v1/auth/reissue")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .cookie(new Cookie("refreshToken", refreshToken)))
                    .andExpect(status().isOk())
                    .andExpect(cookie().value("refreshToken", refreshToken))
                    .andExpect(cookie().httpOnly("refreshToken", true))
                    .andExpect(cookie().sameSite("refreshToken", "Strict"))
                    .andExpect(jsonPath("$.data.accessToken").value(tokenResult.accessToken()));

            verify(authService).reissue(any());
        }

        @Test
        @DisplayName("RefreshToken이 Null일 시 401 예외가 발생한다. ")
        void reissue_fail_when_token_invalid() throws Exception {
            // when & then
            mockMvc.perform(post("/api/v1/auth/reissue").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(
                            jsonPath("$.error.message")
                                    .value(AuthErrorCode.TOKEN_INVALID.message()));

            verify(authService, never()).reissue(any());
        }
    }
}
