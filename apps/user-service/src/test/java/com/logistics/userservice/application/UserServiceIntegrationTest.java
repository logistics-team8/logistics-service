package com.logistics.userservice.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.common.error.CommonErrorCode;
import com.logistics.common.exception.BusinessException;
import com.logistics.infrastructure.config.test.AbstractIntegrationTest;
import com.logistics.infrastructure.config.test.ConcurrencyTestingUtil;
import com.logistics.userservice.application.dto.UserSignUpCommand;
import com.logistics.userservice.domain.Role;
import com.logistics.userservice.domain.User;
import com.logistics.userservice.domain.UserRepository;
import com.logistics.userservice.domain.UserStatus;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
public class UserServiceIntegrationTest extends AbstractIntegrationTest {
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UserService userService;

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
                            "U123456789",
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
            assertThat(passwordEncoder.matches("Testtest123!", savedUser.getPassword())).isTrue();
            assertThat(savedUser.getName()).isEqualTo("김철수");
            assertThat(savedUser.getSlackId()).isEqualTo("U123456789");
            assertThat(savedUser.getRole()).isEqualTo(Role.COMPANY_MANAGER);
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
                            "test1234",
                            "Testtest123!",
                            "김철수",
                            "U123456789",
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
}
