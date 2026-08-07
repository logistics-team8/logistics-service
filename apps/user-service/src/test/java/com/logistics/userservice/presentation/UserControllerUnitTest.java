package com.logistics.userservice.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.logistics.infrastructure.config.test.AbstractControllerTest;
import com.logistics.userservice.application.UserService;
import com.logistics.userservice.domain.Role;
import com.logistics.userservice.presentation.dto.request.SignUpRequest;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(controllers = UserController.class)
class UserControllerUnitTest extends AbstractControllerTest {
    @MockitoBean private UserService userService;

    @Nested
    @DisplayName("회원가입")
    class CreateUser {
        @Test
        @DisplayName("회원가입 성공")
        void createUser_success() throws Exception {
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
        void createUser_fail_when_invalid(SignUpRequest request) throws Exception {
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
}
