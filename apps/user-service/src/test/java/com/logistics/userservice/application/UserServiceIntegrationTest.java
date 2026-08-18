package com.logistics.userservice.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.logistics.common.exception.BusinessException;
import com.logistics.userservice.application.dto.company.CompanyInfo;
import com.logistics.userservice.application.dto.user.*;
import com.logistics.userservice.application.port.CompanyClientPort;
import com.logistics.userservice.application.port.HubClientPort;
import com.logistics.userservice.config.test.AbstractIntegrationTest;
import com.logistics.userservice.domain.RequestedRole;
import com.logistics.userservice.domain.User;
import com.logistics.userservice.domain.UserRepository;
import com.logistics.userservice.domain.UserStatus;
import com.logistics.userservice.domain.redis.RefreshTokenRepository;
import com.logistics.userservice.domain.redis.RoleCacheRepository;
import com.logistics.userservice.domain.redis.SessionRepository;
import com.logistics.userservice.error.UserErrorCode;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@DisplayName("UserService - 통합 테스트")
public class UserServiceIntegrationTest extends AbstractIntegrationTest {
    @Autowired private UserRepository userRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private SessionRepository sessionRepository;
    @Autowired private RoleCacheRepository roleCacheRepository;
    @Autowired private UserService userService;

    @MockitoBean private HubClientPort hubClientPort;
    @MockitoBean private CompanyClientPort companyClientPort;

    private User dummyUser;
    private User dummyUser2;
    private UUID userId;
    private UUID userId2;
    UUID sessionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        UserCreateCommand command =
                new UserCreateCommand(
                        "dummy1234",
                        "Testtest123!",
                        "김철수",
                        "U33333333",
                        null,
                        UUID.randomUUID(),
                        RequestedRole.COMPANY_MANAGER);

        UserCreateCommand command2 =
                new UserCreateCommand(
                        "dummy12345",
                        "Testtest123!",
                        "김철수",
                        "U44444444",
                        null,
                        UUID.randomUUID(),
                        RequestedRole.COMPANY_MANAGER);
        dummyUser = userRepository.saveAndFlush(User.createByAdmin(UUID.randomUUID(), command));

        dummyUser2 = userRepository.saveAndFlush(User.createByAdmin(UUID.randomUUID(), command2));
        userId = dummyUser.getId();
        userId2 = dummyUser2.getId();
    }

    @AfterEach
    void tearDown() {
        if (userId != null) {
            refreshTokenRepository.delete(userId, sessionId);
            roleCacheRepository.delete(userId);
        }

        if (userId2 != null) {
            refreshTokenRepository.delete(userId2, sessionId);
            roleCacheRepository.delete(userId2);
        }

        userRepository.deleteAllInBatch();
    }

    @Nested
    @DisplayName("소속 회원가입 성공 테스트")
    class createUserFromAffiliation_success {
        @Test
        @DisplayName("허브 실존 여부를 검증하고 허브 관리자 회원가입 요청을 성공한다.")
        void createUserFromHub_success() {
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
            userService.createUser(hubCommand);
            User savedUser = userRepository.findByUsername(hubCommand.username()).get();

            // then
            assertThat(savedUser.getUsername()).isEqualTo(hubCommand.username());
            assertThat(savedUser.getRole()).isNull();
            assertThat(savedUser.getRequestedRole()).isEqualTo(hubCommand.requestedRole());
            assertThat(savedUser.getUserStatus()).isEqualTo(UserStatus.PENDING);
        }

        @Test
        @DisplayName("업체 실존 여부를 검증하고 업체 관리자 회원가입을 성공한다.")
        void createUserFromCompany_success() {
            // given
            UUID hubId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();

            UserCreateCommand companyCommand =
                    new UserCreateCommand(
                            "companyuser1234",
                            "Testtest123!",
                            "김철수",
                            "U88888888",
                            null,
                            companyId,
                            RequestedRole.COMPANY_MANAGER);

            CompanyInfo companyInfo = new CompanyInfo(hubId, companyId);
            given(companyClientPort.getCompanyInfo(any())).willReturn(companyInfo);

            // when
            userService.createUser(companyCommand);
            User savedUser = userRepository.findByUsername(companyCommand.username()).get();

            // then
            assertThat(savedUser.getUsername()).isEqualTo(companyCommand.username());
            assertThat(savedUser.getRole()).isNull();
            assertThat(savedUser.getRequestedRole()).isEqualTo(companyCommand.requestedRole());
            assertThat(savedUser.getHubId()).isEqualTo(companyInfo.hubId());
            assertThat(savedUser.getCompanyId()).isEqualTo(companyInfo.companyId());
            assertThat(savedUser.getUserStatus()).isEqualTo(UserStatus.PENDING);
        }

        @Test
        @DisplayName("허브 실존 여부를 검증하고 배송 담당자 회원가입을 성공한다.")
        void createUserFromDelivery_success() {
            // given
            UUID hubId = UUID.randomUUID();

            UserCreateCommand deliveryCommand =
                    new UserCreateCommand(
                            "companyuser1234",
                            "Testtest123!",
                            "김철수",
                            "U88888888",
                            hubId,
                            null,
                            RequestedRole.HUB_DELIVERY);

            given(hubClientPort.existsById(any())).willReturn(true);

            // when
            userService.createUser(deliveryCommand);
            User savedUser = userRepository.findByUsername(deliveryCommand.username()).get();

            // then
            assertThat(savedUser.getUsername()).isEqualTo(deliveryCommand.username());
            assertThat(savedUser.getRole()).isNull();
            assertThat(savedUser.getRequestedRole()).isEqualTo(deliveryCommand.requestedRole());
            assertThat(savedUser.getHubId()).isEqualTo(deliveryCommand.hubId());
            assertThat(savedUser.getUserStatus()).isEqualTo(UserStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("내부 통신 Service")
    class InternalTest {
        @Test
        @DisplayName("회원 정보 조회 성공")
        void getUserInfo_success() {
            // given
            UserInfo savedUserInfo = UserInfo.from(dummyUser);

            // when
            UserInfo userInfo = userService.getUserInfo(userId);

            // then
            assertThat(savedUserInfo.userId()).isEqualTo(userInfo.userId());
            assertThat(savedUserInfo.username()).isEqualTo(userInfo.username());
        }

        @Test
        @DisplayName("회원 권한 조회 성공")
        void getUserRole_success() {
            // given
            UserRoleInfo savedUserRoleInfo = new UserRoleInfo(dummyUser.getRole());

            // when
            UserRoleInfo userRoleInfo = userService.getUserRole(userId);

            // then
            assertThat(savedUserRoleInfo).isEqualTo(userRoleInfo);
        }

        @Test
        @DisplayName("회원 슬랙ID 조회 성공")
        void getUserSlackId_success() {
            // given
            UserSlackInfo savedUserSlackInfo = new UserSlackInfo(dummyUser.getSlackId());

            // when
            UserSlackInfo userSlackInfo = userService.getUserSlackId(userId);

            // then
            assertThat(savedUserSlackInfo).isEqualTo(userSlackInfo);
        }
    }

    @Nested
    @DisplayName("회원 정보 업데이트 테스트")
    class UpdateUser {
        @Test
        @DisplayName("회원 정보 업데이트에 성공한다.")
        void updateUser_success() {
            // given
            UserUpdateCommand updateCommand = new UserUpdateCommand(userId2, "김길동", "U5555555");

            // when
            userService.updateUser(updateCommand);

            User updatedUser = userRepository.findById(userId2).orElseThrow();

            // then
            assertThat(updateCommand.name()).isEqualTo(updatedUser.getName());
            assertThat(updateCommand.slackId()).isEqualTo(updatedUser.getSlackId());
        }

        @Test
        @DisplayName("회원 정보 업데이트 시 중복된 slack Id가 있으면 예외가 발생해야 한다.")
        void updateUser_fail_when_slack_id_duplicate() {
            // given
            UserUpdateCommand updateCommand = new UserUpdateCommand(userId2, "김길동", "U33333333");

            // when & then
            assertThatThrownBy(() -> userService.updateUser(updateCommand))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(UserErrorCode.USER_DUPLICATE_SLACK_ID.message());
        }
    }

    @Test
    @DisplayName("회원 탈퇴에 성공하면 회원 정보와 Redis 인증 정보를 삭제한다.")
    void deleteUser_success() throws InterruptedException {
        // given
        refreshTokenRepository.save(userId, sessionId, "refreshToken");
        roleCacheRepository.save(userId, "master");

        // when
        sessionRepository.save(userId, sessionId, 1);
        userService.deleteUser(userId);

        Thread.sleep(1000);

        // then
        assertThat(sessionRepository.exists(userId, sessionId)).isFalse();
        assertThat(roleCacheRepository.findByUserId(userId).orElse(null)).isNull();

        assertThatThrownBy(() -> userService.deleteUser(userId))
                .isInstanceOf(BusinessException.class)
                .hasMessage(UserErrorCode.USER_NOT_FOUND.message());
    }
}
