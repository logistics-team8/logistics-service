package com.logistics.userservice.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.common.error.CommonErrorCode;
import com.logistics.common.exception.BusinessException;
import com.logistics.infrastructure.config.test.AbstractIntegrationTest;
import com.logistics.infrastructure.config.test.ConcurrencyTestingUtil;
import com.logistics.userservice.application.dto.UserInfo;
import com.logistics.userservice.application.dto.UserRoleInfo;
import com.logistics.userservice.application.dto.UserSignUpCommand;
import com.logistics.userservice.application.dto.UserSlackInfo;
import com.logistics.userservice.domain.Role;
import com.logistics.userservice.domain.User;
import com.logistics.userservice.domain.UserRepository;
import com.logistics.userservice.domain.UserStatus;
import jakarta.persistence.EntityManager;
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
public class UserServiceIntegrationTest extends AbstractIntegrationTest {
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;
    @Autowired private UserService userService;

    private User dummyUser;
    private UUID userId;

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
        dummyUser = userRepository.save(User.create(command));
        userId = dummyUser.getId();
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
    }

    @Nested
    @DisplayName("회원가입 성공 - 통합 테스트")
    class CreateUser {
        @Test
        @Transactional
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

            entityManager.flush();
            entityManager.clear();

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
    @DisplayName("내부 통신 Service - 통합 테스트")
    class InternalTest {
        @Test
        @Transactional
        @DisplayName("회원 정보 조회 성공")
        void getUserInfo_success() {
            // given
            UserInfo savedUserInfo = UserInfo.from(dummyUser);

            // when
            UserInfo userInfo = userService.getUserInfo(userId);

            // then
            assertThat(savedUserInfo).isEqualTo(userInfo);
        }

        @Test
        @Transactional
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
            UserSlackInfo savedUserSlackInfo = new UserSlackInfo(userId, dummyUser.getSlackId());

            // when
            UserSlackInfo userSlackInfo = userService.getUserSlackId(userId);

            // then
            assertThat(savedUserSlackInfo).isEqualTo(userSlackInfo);
        }
    }
}
