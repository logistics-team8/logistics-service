package com.logistics.userservice.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.logistics.common.error.CommonErrorCode;
import com.logistics.common.exception.BusinessException;
import com.logistics.userservice.application.dto.*;
import com.logistics.userservice.config.test.AbstractIntegrationTest;
import com.logistics.userservice.config.test.ConcurrencyTestingUtil;
import com.logistics.userservice.domain.Role;
import com.logistics.userservice.domain.User;
import com.logistics.userservice.domain.UserRepository;
import com.logistics.userservice.domain.UserStatus;
import com.logistics.userservice.domain.redis.RefreshTokenRepository;
import com.logistics.userservice.domain.redis.RoleCacheRepository;
import com.logistics.userservice.error.UserErrorCode;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@DisplayName("UserService - 통합 테스트")
public class UserServiceIntegrationTest extends AbstractIntegrationTest {
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private RoleCacheRepository roleCacheRepository;
    @Autowired private UserService userService;

    private User dummyUser;
    private User dummyUser2;
    private UUID userId;
    private UUID userId2;

    @BeforeEach
    void setUp() {
        UserSignUpCommand command =
                new UserSignUpCommand(
                        "dummy1234",
                        "Testtest123!",
                        "김철수",
                        "U33333333",
                        null,
                        null,
                        Role.COMPANY_MANAGER);

        UserSignUpCommand command2 =
                new UserSignUpCommand(
                        "dummy12345",
                        "Testtest123!",
                        "김철수",
                        "U44444444",
                        null,
                        null,
                        Role.COMPANY_MANAGER);
        dummyUser = userRepository.save(User.create(command));
        dummyUser2 = userRepository.save(User.create(command2));
        userId = dummyUser.getId();
        userId2 = dummyUser2.getId();
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
    }

    @Nested
    @DisplayName("회원가입 성공 - 통합 테스트")
    class CreateUser {
        @Test
        @DisplayName("회원가입 성공")
        void createUser_success() {
            // given
            UserSignUpCommand command =
                    new UserSignUpCommand(
                            "test1234",
                            "Testtest123!",
                            "김철수",
                            "U22222222",
                            null,
                            null,
                            Role.COMPANY_MANAGER);

            // when
            userService.createUser(command);

            User savedUser =
                    userRepository
                            .findByUsernameAndDeletedAtIsNull(command.username())
                            .orElseThrow();

            // then
            assertThat(savedUser.getUsername()).isEqualTo(command.username());
            assertThat(passwordEncoder.matches(command.password(), savedUser.getPassword()))
                    .isTrue();
            assertThat(savedUser.getName()).isEqualTo(command.name());
            assertThat(savedUser.getSlackId()).isEqualTo(command.slackId());
            assertThat(savedUser.getRole()).isEqualTo(command.role());
            assertThat(savedUser.getUserStatus()).isEqualTo(UserStatus.PENDING);
            assertThat(savedUser.getCreatedAt()).isNotNull();
            assertThat(savedUser.getCreatedBy()).isNull();
        }

        @Test
        @DisplayName(("회원가입 시 동시 클릭하는 경우 하나의 케이스만 성공해야 한다."))
        void createUser_fail_when_duplicate_on_concurrency() throws InterruptedException {
            // given
            UserSignUpCommand command =
                    new UserSignUpCommand(
                            "test123456",
                            "Testtest123!",
                            "김철수",
                            "U11111111",
                            null,
                            null,
                            Role.COMPANY_MANAGER);

            int threadCount = 5;
            AtomicInteger successCount = new AtomicInteger(0);

            // when
            List<Exception> failures = new CopyOnWriteArrayList<>();
            ConcurrencyTestingUtil.run(
                    threadCount,
                    () -> {
                        try {
                            userService.createUser(command);
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            failures.add(e);
                        }
                    });

            // then
            assertThat(successCount.get()).isEqualTo(1);
            assertThat(failures).hasSize(4);
            assertThat(failures)
                    .allSatisfy(
                            e -> {
                                assertThat(e).isInstanceOf(BusinessException.class);
                                assertThat(((BusinessException) e).getErrorCode())
                                        .isEqualTo(CommonErrorCode.DUPLICATE_RESOURCE);
                            });
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
        @Transactional
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
    @Transactional
    @DisplayName("회원 탈퇴에 성공하면 회원 정보와 Redis 인증 정보를 삭제한다.")
    void deleteUser_success() {
        // given
        refreshTokenRepository.save(userId, "refreshToken");
        roleCacheRepository.save(userId, "role");

        // when
        userService.deleteUser(userId);

        // then
        assertThat(refreshTokenRepository.findByUserId(userId).orElse(null)).isNull();
        ;
        assertThat(roleCacheRepository.findByUserId(userId).orElse(null)).isNull();
        ;

        assertThatThrownBy(() -> userService.deleteUser(userId))
                .isInstanceOf(BusinessException.class)
                .hasMessage(UserErrorCode.USER_NOT_FOUND.message());
    }
}
