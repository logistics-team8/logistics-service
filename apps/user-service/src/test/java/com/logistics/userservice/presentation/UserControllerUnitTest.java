package com.logistics.userservice.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.logistics.common.error.CommonErrorCode;
import com.logistics.userservice.application.UserService;
import com.logistics.userservice.config.test.AbstractControllerTest;
import com.logistics.userservice.domain.Role;
import com.logistics.userservice.presentation.dto.user.SignUpRequest;
import com.logistics.userservice.presentation.dto.user.UpdateRequest;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DisplayName("UserController - 단위 테스트")
@WebMvcTest(controllers = UserController.class)
class UserControllerUnitTest extends AbstractControllerTest {
    @MockitoBean private UserService userService;

    @Nested
    @DisplayName("회원가입")
    class SignUp {
        @Test
        @DisplayName("회원가입 성공")
        void signUp_success() throws Exception {
            // given
            SignUpRequest request =
                    new SignUpRequest(
                            "test1234",
                            "Testtest123!",
                            "김철수",
                            "U123456789",
                            UUID.randomUUID(),
                            UUID.randomUUID(),
                            Role.COMPANY_MANAGER);

            // when & then
            mockMvc.perform(
                            post("/api/v1/users")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            verify(userService).createUser(any());
        }

        @ParameterizedTest
        @MethodSource("testCase")
        @DisplayName("회원가입 시 유효성 체크를 통과하지 못하면 예외가 발생해야한다.")
        void signUp_fail_when_invalid(SignUpRequest request) throws Exception {
            // when & then
            mockMvc.perform(
                            post("/api/v1/users")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(userService, never()).createUser(any());
        }

        static Stream<SignUpRequest> testCase() {
            return Stream.of(
                    // 1. 아이디 유효성 검사 실패
                    new SignUpRequest(
                            "아이디",
                            "Testtest123!",
                            "김철수",
                            "U123456789",
                            UUID.randomUUID(),
                            UUID.randomUUID(),
                            Role.COMPANY_MANAGER),

                    // 2. 비밀번호 유효성 검사 실패
                    new SignUpRequest(
                            "test1234",
                            "비밀번호",
                            "김철수",
                            "U123456789",
                            UUID.randomUUID(),
                            UUID.randomUUID(),
                            Role.COMPANY_MANAGER),

                    // 3. 이름 유효성 검사 실패
                    new SignUpRequest(
                            "test1234",
                            "Testtest123!",
                            "",
                            "U123456789",
                            UUID.randomUUID(),
                            UUID.randomUUID(),
                            Role.COMPANY_MANAGER),

                    // 4. 슬랙ID 유효성 검사 실패
                    new SignUpRequest(
                            "test1234",
                            "Testtest123!",
                            "김철수",
                            "테스트",
                            UUID.randomUUID(),
                            UUID.randomUUID(),
                            Role.COMPANY_MANAGER),

                    // 5. 허브 ID 유효성 검사 실패
                    new SignUpRequest(
                            "test1234",
                            "Testtest123!",
                            "김철수",
                            "U123456789",
                            null,
                            UUID.randomUUID(),
                            Role.COMPANY_MANAGER),

                    // 6. 권한 유효성 검사 실패
                    new SignUpRequest(
                            "test1234",
                            "Testtest123!",
                            "김철수",
                            "U123456789",
                            UUID.randomUUID(),
                            UUID.randomUUID(),
                            null));
        }
    }

    @Test
    @DisplayName("로그인 하지 않은 유저가 조회 요청 시 401 예외가 발생한다.")
    void getMyInfo_fail_when_unauthorized() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/users/me").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(
                        jsonPath("$.error.message").value(CommonErrorCode.UNAUTHORIZED.message()));

        verify(userService, never()).getUserInfo(any());
    }

    @Nested
    @DisplayName("회원 수정 테스트")
    class UpdateMyInfo {
        @Test
        @DisplayName("로그인 하지 않은 유저가 회원 정보 수정 요청 시 401 예외가 발생한다.")
        void updateMyInfo_fail_when_unauthorized() throws Exception {
            // when & then
            mockMvc.perform(patch("/api/v1/users/me").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(
                            jsonPath("$.error.message")
                                    .value(CommonErrorCode.UNAUTHORIZED.message()));

            verify(userService, never()).updateUser(any());
        }

        @WithMockUser
        @ParameterizedTest
        @MethodSource("updateTestCase")
        @DisplayName("유효성 검사가 실패하면 400 예외가 발생한다.")
        void updateMyInfo_fail_when_invalid() throws Exception {
            // when & then
            mockMvc.perform(patch("/api/v1/users/me").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(
                            jsonPath("$.error.message")
                                    .value(CommonErrorCode.INVALID_INPUT.message()));

            verify(userService, never()).updateUser(any());
        }

        static Stream<UpdateRequest> updateTestCase() {
            return Stream.of(
                    // 1. 아이디 유효성 검사 실패
                    new UpdateRequest("", "U1234567890"),

                    // 2. Slack Id 유효성 검사 실패
                    new UpdateRequest("김철수", "qweqwr213"));
        }
    }

    @Test
    @DisplayName("로그인 하지 않은 유저가 회원 탈퇴 요청 시 401 예외가 발생한다.")
    void deleteMyAccount_fail_when_unauthorized() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/v1/users/me").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(
                        jsonPath("$.error.message").value(CommonErrorCode.UNAUTHORIZED.message()));

        verify(userService, never()).deleteUser(any());
    }
}
