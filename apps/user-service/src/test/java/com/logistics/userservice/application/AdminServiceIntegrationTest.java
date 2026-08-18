package com.logistics.userservice.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.logistics.common.security.principal.CustomUserDetails;
import com.logistics.userservice.application.dto.admin.AdminApprovalCommand;
import com.logistics.userservice.application.dto.admin.AdminRejectCommand;
import com.logistics.userservice.application.dto.company.CompanyInfo;
import com.logistics.userservice.application.dto.user.UserCreateCommand;
import com.logistics.userservice.application.dto.user.UserUpdateCommand;
import com.logistics.userservice.application.port.CompanyClientPort;
import com.logistics.userservice.application.port.DeliveryClientPort;
import com.logistics.userservice.application.port.HubClientPort;
import com.logistics.userservice.config.test.AbstractIntegrationTest;
import com.logistics.userservice.domain.*;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@SpringBootTest
@DisplayName("AdminService - 통합 테스트")
class AdminServiceIntegrationTest extends AbstractIntegrationTest {
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AdminService adminService;
    @Autowired private EntityManager entityManager;

    @MockitoBean private HubClientPort hubClientPort;
    @MockitoBean private CompanyClientPort companyClientPort;
    @MockitoBean private DeliveryClientPort deliveryClientPort;

    private User dummyUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        UserCreateCommand command =
                new UserCreateCommand(
                        "test1234",
                        passwordEncoder.encode("Testtest123!"),
                        "김철수",
                        "U123456789",
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        RequestedRole.COMPANY_MANAGER);
        dummyUser = userRepository.save(User.create(command));
        userId = dummyUser.getId();
    }

    // ============================== CRUD ==============================
    @Nested
    @DisplayName("관리자 회원 생성 테스트")
    class CreateUser {
        @Test
        @DisplayName("MASTER가 허브 존재 유무 확인 후 HUB_MANAGER 회원 생성을 완료한다.")
        void createUser_success_with_hubManager() {
            // given
            UserCreateCommand hubCommand =
                    new UserCreateCommand(
                            "hubuser1234",
                            "Testtest123!",
                            "김철수",
                            "U777777777",
                            UUID.randomUUID(),
                            null,
                            RequestedRole.HUB_MANAGER);

            given(hubClientPort.existsById(any())).willReturn(true);

            // when
            adminService.createUserByAdmin(UUID.randomUUID(), hubCommand);
            User savedUser = userRepository.findByUsername(hubCommand.username()).get();

            // then
            assertThat(savedUser.getUsername()).isEqualTo(hubCommand.username());
            assertThat(savedUser.getRole()).isEqualTo(Role.HUB_MANAGER);
            assertThat(savedUser.getRequestedRole()).isNull();
            assertThat(savedUser.getUserStatus()).isEqualTo(UserStatus.APPROVED);
        }

        @Test
        @DisplayName("MASTER가 업체 존재 유무 확인 후 COMPANY_MANAGER 회원 생성을 완료한다.")
        void createUser_success_with_companyManager() {
            // given
            UUID hubId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();

            UserCreateCommand hubCommand =
                    new UserCreateCommand(
                            "hubuser1234",
                            "Testtest123!",
                            "김철수",
                            "U777777777",
                            null,
                            companyId,
                            RequestedRole.COMPANY_MANAGER);

            CompanyInfo companyInfo = new CompanyInfo(companyId, hubId);

            given(companyClientPort.getCompanyInfo(any())).willReturn(companyInfo);

            // when
            adminService.createUserByAdmin(UUID.randomUUID(), hubCommand);
            User savedUser = userRepository.findByUsername(hubCommand.username()).get();

            // then
            assertThat(savedUser.getUsername()).isEqualTo(hubCommand.username());
            assertThat(savedUser.getRole()).isEqualTo(Role.COMPANY_MANAGER);
            assertThat(savedUser.getRequestedRole()).isNull();
            assertThat(savedUser.getUserStatus()).isEqualTo(UserStatus.APPROVED);
        }

        @Test
        @DisplayName("MASTER가 허브 존재 유무 확인 후 배송 담당자 권한 회원 생성을 완료한다.")
        void createUser_success_with_deliveryManager() throws InterruptedException {
            // given
            UserCreateCommand deliveryCommand =
                    new UserCreateCommand(
                            "hubuser1234",
                            "Testtest123!",
                            "김철수",
                            "U777777777",
                            UUID.randomUUID(),
                            null,
                            RequestedRole.HUB_DELIVERY);

            given(hubClientPort.existsById(any())).willReturn(true);

            // when
            adminService.createUserByAdmin(UUID.randomUUID(), deliveryCommand);

            User savedUser = userRepository.findByUsername(deliveryCommand.username()).get();

            // then
            assertThat(savedUser.getUsername()).isEqualTo(deliveryCommand.username());
            assertThat(savedUser.getRole()).isNull();
            assertThat(savedUser.getRequestedRole()).isEqualTo(RequestedRole.HUB_DELIVERY);
            assertThat(savedUser.getUserStatus()).isEqualTo(UserStatus.PROCESSING);
        }
    }

    @Test
    @DisplayName("MASTER가 회원을 수정한다.")
    void updateUser_success() {
        // given
        UserUpdateCommand command = new UserUpdateCommand(userId, "수정이름", "U22222222");

        // when
        adminService.updateUser(command);

        entityManager.flush();
        entityManager.clear();

        User updatedUser = userRepository.findById(userId).get();

        // then
        assertThat(updatedUser.getName()).isEqualTo("수정이름");
        assertThat(updatedUser.getSlackId()).isEqualTo("U22222222");
    }

    @Test
    @DisplayName("MASTER가 회원을 삭제한다.")
    void deleteUser_success() {
        // when
        adminService.deleteUser(userId);

        entityManager.flush();
        entityManager.clear();

        // then
        assertThat(userRepository.findByIdAndDeletedAtIsNull(userId)).isEmpty();

        User deletedUser = userRepository.findById(userId).orElseThrow();

        assertThat(deletedUser.getDeletedAt()).isNotNull();
    }

    // ============================== Approval ==============================
    @Nested
    @DisplayName("회원가입 요청 승인 테스트")
    class approveUser {
        @Test
        @DisplayName("MASTER가 허브 관리자 회원가입 요청을 승인한다.")
        void approveUser_success_hubManager() {
            // given
            UserCreateCommand hubCommand =
                    new UserCreateCommand(
                            "hub1234",
                            passwordEncoder.encode("Testtest123!"),
                            "김철수",
                            "U153426789",
                            UUID.randomUUID(),
                            null,
                            RequestedRole.HUB_MANAGER);

            User hubUser = userRepository.save(User.create(hubCommand));

            UUID adminId = UUID.randomUUID();
            UUID hubId = hubUser.getHubId();

            CustomUserDetails principal = CustomUserDetails.from(adminId, null, null, "MASTER");
            AdminApprovalCommand command = AdminApprovalCommand.of(principal, hubUser.getId());

            given(hubClientPort.existsById(hubId)).willReturn(true);

            // when
            adminService.approveUser(command);
            User approeUser = userRepository.findById(hubUser.getId()).get();

            // then
            assertThat(approeUser.getRole()).isEqualTo(Role.HUB_MANAGER);
            assertThat(approeUser.getRequestedRole()).isNull();
            assertThat(approeUser.getHubId()).isEqualTo(hubId);
            assertThat(approeUser.getUserStatus()).isEqualTo(UserStatus.APPROVED);
            assertThat(approeUser.getApprovedAt()).isNotNull();
            assertThat(approeUser.getApprovedBy()).isEqualTo(adminId);
        }

        @Test
        @DisplayName("MASTER가 업체 관리자 회원가입 요청을 승인한다.")
        void approveUser_success_companyManager() {
            // given
            UUID adminId = UUID.randomUUID();
            UUID hubId = dummyUser.getHubId();
            UUID companyId = dummyUser.getCompanyId();

            CustomUserDetails principal = CustomUserDetails.from(adminId, null, null, "MASTER");
            AdminApprovalCommand command = AdminApprovalCommand.of(principal, userId);

            CompanyInfo companyInfo = new CompanyInfo(companyId, hubId);

            given(companyClientPort.getCompanyInfo(companyId)).willReturn(companyInfo);

            // when
            adminService.approveUser(command);
            User approeUser = userRepository.findById(userId).get();

            // then
            assertThat(approeUser.getRole()).isEqualTo(Role.COMPANY_MANAGER);
            assertThat(approeUser.getRequestedRole()).isNull();
            assertThat(approeUser.getHubId()).isEqualTo(hubId);
            assertThat(approeUser.getCompanyId()).isEqualTo(companyInfo.companyId());
            assertThat(approeUser.getUserStatus()).isEqualTo(UserStatus.APPROVED);
            assertThat(approeUser.getApprovedAt()).isNotNull();
            assertThat(approeUser.getApprovedBy()).isEqualTo(adminId);
        }

        @Test
        @DisplayName("MASTER가 배송 담당자 회원가입 요청을 승인한다.")
        void approveUser_success_deliveryManager() {
            // given
            UserCreateCommand deliveryCommand =
                    new UserCreateCommand(
                            "delivery1234",
                            passwordEncoder.encode("Testtest123!"),
                            "김철수",
                            "U153456789",
                            UUID.randomUUID(),
                            null,
                            RequestedRole.HUB_DELIVERY);

            User deliveryUser = userRepository.save(User.create(deliveryCommand));
            UUID deliveryUserId = deliveryUser.getId();

            UUID adminId = UUID.randomUUID();
            UUID hubId = deliveryUser.getHubId();

            CustomUserDetails principal = CustomUserDetails.from(adminId, null, null, "MASTER");
            AdminApprovalCommand command = AdminApprovalCommand.of(principal, deliveryUserId);

            given(hubClientPort.existsById(hubId)).willReturn(true);

            // when
            adminService.approveUser(command);
            User approeUser = userRepository.findById(deliveryUserId).get();

            // then
            assertThat(approeUser.getRole()).isNull();
            assertThat(approeUser.getRequestedRole()).isEqualTo(RequestedRole.HUB_DELIVERY);
            assertThat(approeUser.getHubId()).isEqualTo(hubId);
            assertThat(approeUser.getUserStatus()).isEqualTo(UserStatus.PROCESSING);
            assertThat(approeUser.getApprovedAt()).isNotNull();
            assertThat(approeUser.getApprovedBy()).isEqualTo(adminId);
        }
    }

    @Test
    @DisplayName("MASTER가 회원가입 요청을 거절한다.")
    void rejectUser_success() {
        // given
        UUID adminId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();

        AdminRejectCommand command =
                new AdminRejectCommand(adminId, Role.MASTER, hubId, userId, "거절사유");

        // when
        adminService.rejectUser(command);
        User approeUser = userRepository.findById(userId).get();

        // then
        assertThat(approeUser.getApprovedBy()).isEqualTo(command.adminId());
        assertThat(approeUser.getRejectionReason()).isEqualTo(command.reason());
    }
}
