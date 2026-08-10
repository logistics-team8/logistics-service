package com.logistics.userservice.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.logistics.common.security.principal.CustomUserDetails;
import com.logistics.userservice.application.AdminService;
import com.logistics.userservice.config.test.AbstractControllerTest;
import com.logistics.userservice.domain.Role;
import com.logistics.userservice.presentation.dto.user.UserCreateRequest;
import com.logistics.userservice.presentation.dto.user.UserUpdateRequest;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DisplayName("AdminControllerTest - 단위 테스트")
@WebMvcTest(controllers = AdminController.class)
class AdminControllerUnitTest extends AbstractControllerTest {
    @MockitoBean private AdminService adminService;

    private CustomUserDetails createPrincipal(Role role) {
        return CustomUserDetails.from(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), role.name());
    }

    // ============================== CRUD ==============================
    @Nested
    @DisplayName("관리자 회원 생성 테스트")
    class CreateUser {
        @Test
        @DisplayName("Master 권한이 아닌 회원은 회원 생성을 할 수 없다.")
        void createUser_fail_when_role_is_not_master() throws Exception {
            // when & then
            mockMvc.perform(
                            post("/api/v1/admin/users")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .with(user(createPrincipal(Role.HUB_MANAGER))))
                    .andExpect(status().isForbidden());
            verify(adminService, never()).createUserByAdmin(any(), any());
        }

        @ParameterizedTest
        @MethodSource("createUserRequests")
        @DisplayName("유효성 체크를 통과하지 못하면 400 예외가 발생해야한다.")
        void createUser_fail_when_invalid(UserCreateRequest request) throws Exception {
            // when & then
            mockMvc.perform(
                            post("/api/v1/admin/users")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .with(user(createPrincipal(Role.MASTER)))
                                    .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
            verify(adminService, never()).createUserByAdmin(any(), any());
        }

        private static Stream<Arguments> createUserRequests() {
            return Stream.of(
                    Arguments.of(
                            new UserCreateRequest(
                                    "",
                                    "Test1234!",
                                    "김길동",
                                    "U12345678",
                                    UUID.randomUUID(),
                                    UUID.randomUUID(),
                                    Role.COMPANY_MANAGER)),
                    Arguments.of(
                            new UserCreateRequest(
                                    "test1234",
                                    "!",
                                    "김길동",
                                    "U12345678",
                                    UUID.randomUUID(),
                                    UUID.randomUUID(),
                                    Role.COMPANY_MANAGER)),
                    Arguments.of(
                            new UserCreateRequest(
                                    "test1234",
                                    "Test1234!",
                                    "123456789012345678901234567890123456789012345678901234567890",
                                    "U12345678",
                                    UUID.randomUUID(),
                                    UUID.randomUUID(),
                                    Role.COMPANY_MANAGER)),
                    Arguments.of(
                            new UserCreateRequest(
                                    "test1234",
                                    "Test1234!",
                                    "김길동",
                                    "Q12345678",
                                    UUID.randomUUID(),
                                    UUID.randomUUID(),
                                    Role.COMPANY_MANAGER)),
                    Arguments.of(
                            new UserCreateRequest(
                                    "test1234",
                                    "Test1234!",
                                    "김길동",
                                    "U12345678",
                                    null,
                                    UUID.randomUUID(),
                                    Role.COMPANY_MANAGER)),
                    Arguments.of(
                            new UserCreateRequest(
                                    "test1234",
                                    "Test1234!",
                                    "김길동",
                                    "U12345678",
                                    UUID.randomUUID(),
                                    UUID.randomUUID(),
                                    null)));
        }
    }

    @Nested
    @DisplayName("관리자 검색 기능 테스트")
    class SearchUsers {
        @Test
        @DisplayName("Master 권한이 아닌 회원은 회원 검색을 할 수 없다.")
        void searchUsers_fail_when_role_is_not_master() throws Exception {
            // when & then
            mockMvc.perform(
                            get("/api/v1/admin/users")
                                    .with(user(createPrincipal(Role.HUB_MANAGER))))
                    .andExpect(status().isForbidden());
            verify(adminService, never()).getUsersInfo(any(), any(), any());
        }

        @ParameterizedTest
        @MethodSource("searchRequests")
        @DisplayName("유효성 체크를 통과하지 못하면 400 예외가 발생해야한다.")
        void searchUsers_fail_when_invalid(String username, String name) throws Exception {
            // when & then
            mockMvc.perform(
                            get("/api/v1/admin/users")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .with(user(createPrincipal(Role.MASTER)))
                                    .param("username", username)
                                    .param("name", name))
                    .andExpect(status().isBadRequest());
            verify(adminService, never()).getUsersInfo(any(), any(), any());
        }

        private static Stream<Arguments> searchRequests() {
            return Stream.of(
                    Arguments.of("t1234567890", "김길동"),
                    Arguments.of(
                            "test1234",
                            "123456789012345678901234567890123456789012345678901234567890"));
        }
    }

    @Nested
    @DisplayName("관리자 회원 조회 기능 테스트")
    class GetUserById {
        @Test
        @DisplayName("Master 권한이 아닌 회원은 회원 조회을 할 수 없다.")
        void getUserById_fail_when_role_is_not_master() throws Exception {
            // when & then
            mockMvc.perform(
                            get("/api/v1/admin/users/{userId}", UUID.randomUUID())
                                    .with(user(createPrincipal(Role.HUB_MANAGER))))
                    .andExpect(status().isForbidden());
            verify(adminService, never()).getUserInfo(any());
        }

        @Test
        @DisplayName("잘못된 파라미터를 보내면 400 예외가 발생해야한다.")
        void getUserById_fail_when_invalid() throws Exception {
            // when & then
            mockMvc.perform(
                            get("/api/v1/admin/users/{userId}", "1231231")
                                    .with(user(createPrincipal(Role.MASTER))))
                    .andExpect(status().isBadRequest());
            verify(adminService, never()).getUserInfo(any());
        }
    }

    @Nested
    @DisplayName("관리자 회원 수정 기능 테스트")
    class UpdateUser {
        @Test
        @DisplayName("Master 권한이 아닌 회원은 회원 수정을 할 수 없다.")
        void updateUser_fail_when_role_is_not_master() throws Exception {
            // when & then
            mockMvc.perform(
                            patch("/api/v1/admin/users/{userId}", UUID.randomUUID())
                                    .with(user(createPrincipal(Role.HUB_MANAGER))))
                    .andExpect(status().isForbidden());
            verify(adminService, never()).updateUser(any());
        }

        @Test
        @DisplayName("잘못된 파라미터를 보내면 400 예외가 발생해야한다.")
        void updateUser_fail_when_invalid_uuid() throws Exception {
            // when & then
            mockMvc.perform(
                            patch("/api/v1/admin/users/{userId}", "파라미터")
                                    .with(user(createPrincipal(Role.MASTER))))
                    .andExpect(status().isBadRequest());
            verify(adminService, never()).updateUser(any());
        }

        @ParameterizedTest
        @MethodSource("updateUserRequests")
        @DisplayName("유효성 체크를 통과하지 못하면 400 예외가 발생해야한다.")
        void updateUser_fail_when_invalid(UserUpdateRequest request) throws Exception {
            // when & then
            mockMvc.perform(
                            patch("/api/v1/admin/users/{userId}", UUID.randomUUID())
                                    .with(user(createPrincipal(Role.MASTER)))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
            verify(adminService, never()).updateUser(any());
        }

        private static Stream<Arguments> updateUserRequests() {
            return Stream.of(
                    Arguments.of(new UserUpdateRequest("", "U1234567890")),
                    Arguments.of(
                            new UserUpdateRequest(
                                    "123456789012345678901234567890123456789012345678901234567890",
                                    "U1234567890")),
                    Arguments.of(new UserUpdateRequest("test1234", null)),
                    Arguments.of(new UserUpdateRequest("test1234", "X1234567890")));
        }
    }

    @Nested
    @DisplayName("관리자 회원 삭제 기능 테스트")
    class deleteUser {
        @Test
        @DisplayName("Master 권한이 아닌 회원은 회원 삭제를 할 수 없다.")
        void deleteUser_fail_when_role_is_not_master() throws Exception {
            // when & then
            mockMvc.perform(
                            delete("/api/v1/admin/users/{userId}", UUID.randomUUID())
                                    .with(user(createPrincipal(Role.HUB_MANAGER))))
                    .andExpect(status().isForbidden());
            verify(adminService, never()).deleteUser(any());
        }

        @Test
        @DisplayName("잘못된 파라미터를 보내면 400 예외가 발생해야한다.")
        void deleteUser_fail_when_invalid_uuid() throws Exception {
            // when & then
            mockMvc.perform(
                            delete("/api/v1/admin/users/{userId}", "파라미터")
                                    .with(user(createPrincipal(Role.MASTER))))
                    .andExpect(status().isBadRequest());
            verify(adminService, never()).deleteUser(any());
        }
    }

    // ============================== Approval ==============================
    @Nested
    @DisplayName("회원 가입 요청 승인 테스트")
    class ApproveUser {
        @Test
        @DisplayName("Master와 Hub_Manage를 제외한 유저는 가입 요청 승인이 불가능하다.")
        void approveUser_fail_when_role_is_not_master_and_hub_manage() throws Exception {
            // when & then
            mockMvc.perform(
                            patch("/api/v1/admin/users/{userId}/approve", UUID.randomUUID())
                                    .with(user(createPrincipal(Role.COMPANY_MANAGER))))
                    .andExpect(status().isForbidden());
            verify(adminService, never()).approveUser(any());
        }

        @Test
        @DisplayName("잘못된 파라미터를 보내면 400 예외가 발생해야한다.")
        void approveUser_fail_when_invalid_uuid() throws Exception {
            // when & then
            mockMvc.perform(
                            patch("/api/v1/admin/users/{userId}/approve", "파라미터")
                                    .with(user(createPrincipal(Role.MASTER))))
                    .andExpect(status().isBadRequest());
            verify(adminService, never()).approveUser(any());
        }
    }

    @Nested
    @DisplayName("회원 가입 요청 거절 테스트")
    class RejectUser {
        @Test
        @DisplayName("Master와 Hub_Manage를 제외한 유저는 가입 요청 거절이 불가능하다.")
        void rejectUser_fail_when_role_is_not_master_and_hub_manage() throws Exception {
            // when & then
            mockMvc.perform(
                            patch("/api/v1/admin/users/{userId}/reject", UUID.randomUUID())
                                    .with(user(createPrincipal(Role.COMPANY_MANAGER))))
                    .andExpect(status().isForbidden());
            verify(adminService, never()).rejectUser(any());
        }

        @Test
        @DisplayName("잘못된 파라미터를 보내면 400 예외가 발생해야한다.")
        void rejectUser_fail_when_invalid_uuid() throws Exception {
            // when & then
            mockMvc.perform(
                            patch("/api/v1/admin/users/{userId}", "파라미터")
                                    .with(user(createPrincipal(Role.MASTER))))
                    .andExpect(status().isBadRequest());
            verify(adminService, never()).rejectUser(any());
        }
    }

    @Nested
    @DisplayName("회원 가입 요청 목록 조회 테스트")
    class GetPendingUsers {
        @Test
        @DisplayName("Master와 Hub_Manage를 제외한 유저는 가입 요청 조회가 불가능하다.")
        void getPendingUsers_fail_when_role_is_not_master_and_hub_manage() throws Exception {
            // when & then
            mockMvc.perform(
                            get("/api/v1/admin/users/pending-approvals", UUID.randomUUID())
                                    .with(user(createPrincipal(Role.COMPANY_MANAGER))))
                    .andExpect(status().isForbidden());
            verify(adminService, never()).getPendingUsers(any(), any(), any());
        }

        @ParameterizedTest
        @MethodSource("searchRequests")
        @DisplayName("유효성 체크를 통과하지 못하면 400 예외가 발생해야한다.")
        void searchUsers_fail_when_invalid(String username, String name) throws Exception {
            // when & then
            mockMvc.perform(
                            get("/api/v1/admin/users")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .with(user(createPrincipal(Role.MASTER)))
                                    .param("username", username)
                                    .param("name", name))
                    .andExpect(status().isBadRequest());
            verify(adminService, never()).getPendingUsers(any(), any(), any());
        }

        private static Stream<Arguments> searchRequests() {
            return Stream.of(
                    Arguments.of("t1234567890", "김길동"),
                    Arguments.of(
                            "test1234",
                            "123456789012345678901234567890123456789012345678901234567890"));
        }
    }
}
