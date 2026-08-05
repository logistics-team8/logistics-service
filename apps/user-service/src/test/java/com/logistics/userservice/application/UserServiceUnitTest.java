package com.logistics.userservice.application;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.logistics.common.exception.BusinessException;
import com.logistics.userservice.application.dto.UserSignUpCommand;
import com.logistics.userservice.domain.Role;
import com.logistics.userservice.domain.User;
import com.logistics.userservice.domain.UserRepository;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {
    @Mock private UserRepository userRepository;
    @InjectMocks private UserService userService;

    @Nested
    @DisplayName("회원가입 실패 테스트")
    class createUser {
        @Test
        @DisplayName("이미 존재하는 아이디일 시 예외가 발생해야한다.")
        void createMember_fail_when_username_is_duplicate() {
            // given
            UserSignUpCommand command =
                    new UserSignUpCommand(
                            "test1234",
                            "Testtest123!",
                            "김철수",
                            "U123456789",
                            UUID.randomUUID(),
                            UUID.randomUUID(),
                            Role.COMPANY_MANAGER);

            User existUsers = User.create(command);

            given(userRepository.findByUsernameOrSlackId(command.username(), command.slackId())).willReturn(List.of(existUsers));

            // when & then
            assertThatThrownBy(() -> userService.createUser(command))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("이미 사용중인 아이디입니다.");

            verify(userRepository).findByUsernameOrSlackId(command.username(), command.slackId());
            verify(userRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("이미 존재하는 Slack 아이디일 시 예외가 발생해야한다.")
        void createMember_fail_when_slack_id_is_duplicate() {
            // given
            UserSignUpCommand command =
                    new UserSignUpCommand(
                            "test1234",
                            "Testtest123!",
                            "김철수",
                            "U123456789",
                            UUID.randomUUID(),
                            UUID.randomUUID(),
                            Role.COMPANY_MANAGER);

            UserSignUpCommand command2 =
                    new UserSignUpCommand(
                            "test12345",
                            "Testtest123!",
                            "김철수",
                            "U123456789",
                            UUID.randomUUID(),
                            UUID.randomUUID(),
                            Role.COMPANY_MANAGER);

            User existUsers = User.create(command2);
            given(userRepository.findByUsernameOrSlackId(command.username(), command.slackId())).willReturn(List.of(existUsers));

            // when & then
            assertThatThrownBy(() -> userService.createUser(command))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("이미 사용중인 Slack 아이디입니다.");

            verify(userRepository).findByUsernameOrSlackId(command.username(), command.slackId());
            verify(userRepository, never()).saveAndFlush(any());
        }
    }
}
