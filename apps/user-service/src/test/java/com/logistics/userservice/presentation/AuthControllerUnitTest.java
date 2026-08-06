package com.logistics.userservice.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.logistics.infrastructure.config.test.AbstractControllerTest;
import com.logistics.userservice.application.AuthService;
import com.logistics.userservice.application.dto.TokenResult;
import com.logistics.userservice.presentation.dto.request.LoginRequest;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(controllers = AuthController.class)
class AuthControllerUnitTest extends AbstractControllerTest {
    @MockitoBean private AuthService authService;

    @Nested
    @DisplayName("로그인 테스트 - Controller")
    class Login {
        @Test
        @DisplayName("로그인 성공")
        void login_success() throws Exception {
            // given
            LoginRequest loginRequest = new LoginRequest("test1234", "Testtest123");
            TokenResult tokenResult = new TokenResult("accessToken", "refreshToken");

            given(authService.login(any(LoginRequest.class))).willReturn(tokenResult);

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

            verify(authService).login(any(LoginRequest.class));
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
}
