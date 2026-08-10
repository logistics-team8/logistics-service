package com.logistics.userservice.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.userservice.application.dto.admin.AdminApprovalCommand;
import com.logistics.userservice.application.dto.admin.AdminRejectCommand;
import com.logistics.userservice.application.dto.user.UserCreateCommand;
import com.logistics.userservice.application.dto.user.UserUpdateCommand;
import com.logistics.userservice.config.test.AbstractIntegrationTest;
import com.logistics.userservice.domain.Role;
import com.logistics.userservice.domain.User;
import com.logistics.userservice.domain.UserRepository;
import com.logistics.userservice.domain.UserStatus;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@SpringBootTest
@DisplayName("AdminService - 통합 테스트")
class AdminServiceIntegrationTest extends AbstractIntegrationTest {
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AdminService adminService;
    @Autowired private EntityManager entityManager;

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
                        null,
                        null,
                        Role.COMPANY_MANAGER);
        dummyUser = userRepository.save(User.create(command));
        userId = dummyUser.getId();
    }

    // ============================== CRUDl ==============================
    @Test
    @DisplayName("관리자가 회원 생성 시 즉시 승인되고 승인자 기록을 남긴다.")
    void createUserByAdmin_success() {
        // given
        UUID adminId = UUID.randomUUID();

        UserCreateCommand command =
                new UserCreateCommand(
                        "admin1234",
                        passwordEncoder.encode("Testtest123!"),
                        "관리자",
                        "U111111111",
                        null,
                        null,
                        Role.COMPANY_MANAGER);

        // when
        adminService.createUserByAdmin(adminId, command);
        User savedUser = userRepository.findByUsername(command.username()).get();

        // then
        assertThat(savedUser.getUserStatus()).isEqualTo(UserStatus.APPROVED);
        assertThat(savedUser.getApprovedBy()).isEqualTo(adminId);
        assertThat(savedUser.getApprovedAt()).isNotNull();
    }

    @Test
    @DisplayName("관리자가 회원을 수정한다.")
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
    @DisplayName("관리자가 회원을 삭제한다.")
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
    @Test
    @DisplayName("관리자가 회원가입 요청을 승인한다.")
    void approveUser_success() {
        // given
        UUID adminId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();

        AdminApprovalCommand command =
                new AdminApprovalCommand(adminId, Role.MASTER, hubId, userId);

        // when
        adminService.approveUser(command);
        User approeUser = userRepository.findById(userId).get();

        // then
        assertThat(approeUser.getApprovedBy()).isEqualTo(command.adminId());
    }

    @Test
    @DisplayName("관리자가 회원가입 요청을 거절한다.")
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
