package com.logistics.userservice.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.logistics.common.error.CommonErrorCode;
import com.logistics.common.exception.BusinessException;
import com.logistics.infrastructure.config.test.AbstractControllerTest;
import com.logistics.userservice.application.UserService;
import com.logistics.userservice.application.dto.UserInfo;
import com.logistics.userservice.application.dto.UserRoleInfo;
import com.logistics.userservice.application.dto.UserSlackInfo;
import com.logistics.userservice.domain.Role;
import com.logistics.userservice.presentation.exception.UserErrorCode;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(InternalUserController.class)
class InternalUserControllerTest extends AbstractControllerTest {
    @MockitoBean private UserService userService;

    @Test
    @DisplayName("회원 정보 조회 성공")
    void getUserInfo() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        UserInfo userInfo =
                new UserInfo(
                        userId,
                        "test1234",
                        "김철수",
                        "U12345678",
                        null,
                        null,
                        Role.COMPANY_MANAGER,
                        LocalDateTime.now());

        given(userService.getUserInfo(eq(userId))).willReturn(userInfo);

        String jsonUserInfo = jsonMapper.writeValueAsString(userInfo);

        // when & then
        mockMvc.perform(
                        get("/internal/v1/users/{userId}", userId)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(jsonUserInfo));

        verify(userService).getUserInfo(eq(userId));
    }

    @Test
    @DisplayName("회원 정보 조회 실패 - 잘못된 입력 값")
    void getUserInfo_fail_when_bad_request() throws Exception {
        // when & then
        mockMvc.perform(
                        get("/internal/v1/users/{userId}", "틀린값")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.error.code").value(CommonErrorCode.INVALID_INPUT.code()))
                .andExpect(
                        jsonPath("$.error.message").value(CommonErrorCode.INVALID_INPUT.message()));

        verify(userService, never()).getUserInfo(any());
    }

    @Test
    @DisplayName("회원 정보 조회 실패 - 존재하지 않는 회원")
    void getUserInfo_fail_when_not_found() throws Exception {
        // given
        UUID userId = UUID.randomUUID();

        given(userService.getUserInfo(eq(userId)))
                .willThrow(new BusinessException(UserErrorCode.USER_NOT_FOUND));

        // when & then
        mockMvc.perform(
                        get("/internal/v1/users/{userId}", userId)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.error.code").value(UserErrorCode.USER_NOT_FOUND.code()))
                .andExpect(
                        jsonPath("$.error.message").value(UserErrorCode.USER_NOT_FOUND.message()));
    }

    @Test
    @DisplayName("회원 권한 조회 성공")
    void getUserRole_success() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        UserRoleInfo userRoleInfo = new UserRoleInfo(userId, Role.COMPANY_MANAGER);

        given(userService.getUserRole(eq(userId))).willReturn(userRoleInfo);

        String jsonUserInfo = jsonMapper.writeValueAsString(userRoleInfo);

        // when & then
        mockMvc.perform(
                        get("/internal/v1/users/{userId}/role", userId)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(jsonUserInfo));

        verify(userService).getUserRole(eq(userId));
    }

    @Test
    @DisplayName("슬랙ID 조회 성공")
    void getUserSlackId_success() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        UserSlackInfo userSlackInfo = new UserSlackInfo(userId, "U12345678");

        given(userService.getUserSlackId(eq(userId))).willReturn(userSlackInfo);

        String jsonUserInfo = jsonMapper.writeValueAsString(userSlackInfo);

        // when & then
        mockMvc.perform(
                        get("/internal/v1/users/{userId}/slack", userId)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(jsonUserInfo));

        verify(userService).getUserSlackId(eq(userId));
    }
}
