package com.logistics.userservice.application.validator;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.logistics.common.exception.BusinessException;
import com.logistics.userservice.application.dto.user.UserCreateCommand;
import com.logistics.userservice.application.port.CompanyClientPort;
import com.logistics.userservice.application.port.HubClientPort;
import com.logistics.userservice.domain.RequestedRole;
import com.logistics.userservice.domain.User;
import com.logistics.userservice.domain.UserRepository;
import com.logistics.userservice.error.UserErrorCode;
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
@DisplayName("UserValidator - 단위 테스트")
class UserValidatorTest {
    @Mock private UserRepository userRepository;
    @Mock private HubClientPort hubClientPort;
    @Mock private CompanyClientPort companyClientPort;
    @InjectMocks private UserValidator validator;

    @Nested
    @DisplayName("회원 가입 중복 체크 로직")
    class ValidateDuplicate {
        @Test
        @DisplayName("중복 검사 성공")
        void validateDuplicate_success() {
            // given
            UserCreateCommand command =
                    new UserCreateCommand(
                            "test1234",
                            "비밀번호",
                            "김철수",
                            "U123456780",
                            null,
                            UUID.randomUUID(),
                            RequestedRole.COMPANY_MANAGER);

            given(userRepository.findByUsernameOrSlackId(any(), any())).willReturn(List.of());

            // when & then
            validator.validateDuplicate(command);
        }

        @Test
        @DisplayName("아이디가 중복인 경우 ")
        void validateDuplicate_fail_when_duplicate_username() {
            // given
            UserCreateCommand command =
                    new UserCreateCommand(
                            "test1234",
                            "비밀번호",
                            "김철수",
                            "U123456780",
                            null,
                            UUID.randomUUID(),
                            RequestedRole.COMPANY_MANAGER);

            UserCreateCommand command2 =
                    new UserCreateCommand(
                            "test1234",
                            "비밀번호",
                            "김철수",
                            "U123456789",
                            null,
                            UUID.randomUUID(),
                            RequestedRole.COMPANY_MANAGER);

            User findUser = User.create(command2);

            given(userRepository.findByUsernameOrSlackId(any(), any()))
                    .willReturn(List.of(findUser));

            // when & then
            assertThatThrownBy(() -> validator.validateDuplicate(command))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(UserErrorCode.USER_DUPLICATE_USERNAME.message());
        }

        @Test
        @DisplayName("슬랙 ID가 중복인 경우 USER_DUPLICATE_SLACK_ID 예외가 발생한다")
        void validateDuplicate_fail_when_duplicate_slackId() {
            // given
            UserCreateCommand command =
                    new UserCreateCommand(
                            "test1234",
                            "비밀번호",
                            "김철수",
                            "U123456780",
                            null,
                            UUID.randomUUID(),
                            RequestedRole.COMPANY_MANAGER);

            UserCreateCommand command2 =
                    new UserCreateCommand(
                            "test12345",
                            "비밀번호",
                            "김철수",
                            "U123456780",
                            null,
                            UUID.randomUUID(),
                            RequestedRole.COMPANY_MANAGER);

            User findUser = User.create(command2);

            given(userRepository.findByUsernameOrSlackId(any(), any()))
                    .willReturn(List.of(findUser));

            // when & then
            assertThatThrownBy(() -> validator.validateDuplicate(command))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(UserErrorCode.USER_DUPLICATE_SLACK_ID.message());
        }
    }

    @Nested
    @DisplayName("회원 가입 요청 시 허브, 업체 유효성 검증")
    class ValidateSignUpAffiliation {
        @Test
        @DisplayName("회원 가입 요청 시 허브, 업체 유효성 검증")
        void validateSignUpAffiliation() {}
    }

    @Test
    @DisplayName("허브관리자가 회원 가입 승인 시 허브 소속 확인")
    void validateManagerPermission() {}

    @Test
    @DisplayName("허브, 업체 유효성 검증")
    void validateAffiliation() {}
}
