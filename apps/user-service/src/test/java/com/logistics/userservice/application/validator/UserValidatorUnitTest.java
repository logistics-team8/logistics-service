package com.logistics.userservice.application.validator;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.logistics.common.error.CommonErrorCode;
import com.logistics.common.exception.BusinessException;
import com.logistics.userservice.application.dto.company.CompanyInfo;
import com.logistics.userservice.application.dto.user.UserCreateCommand;
import com.logistics.userservice.application.port.CompanyClientPort;
import com.logistics.userservice.application.port.HubClientPort;
import com.logistics.userservice.domain.RequestedRole;
import com.logistics.userservice.domain.Role;
import com.logistics.userservice.domain.User;
import com.logistics.userservice.domain.UserRepository;
import com.logistics.userservice.error.ClientErrorCode;
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
class UserValidatorUnitTest {
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
            assertThatCode(() -> validator.validateDuplicate(command)).doesNotThrowAnyException();

            verify(userRepository).findByUsernameOrSlackId(any(), any());
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
    @DisplayName("회원 가입 요청 시 허브, 업체 실존 여부 검증")
    class ValidateSignUpAffiliation {
        @Test
        @DisplayName("허브 유효성 검증에 성공하면 NULL을 반환한다.")
        void validateSignUpAffiliation_success_with_hub() {
            // given
            UserCreateCommand command =
                    new UserCreateCommand(
                            "test1234",
                            "비밀번호",
                            "김철수",
                            "U123456780",
                            UUID.randomUUID(),
                            null,
                            RequestedRole.HUB_MANAGER);

            given(hubClientPort.existsById(any())).willReturn(true);

            // when
            var result = validator.validateSignUpAffiliation(command);

            // then
            assertThat(result).isNull();

            verify(hubClientPort).existsById(any());
            verify(companyClientPort, never()).getCompanyInfo(any());
        }

        @Test
        @DisplayName("입력 받은 허브의 ID가 존재하지 않으면 HUB_NOT_FOUND 예외가 발생한다.")
        void validateSignUpAffiliation_fail_when_hub_not_found() {
            // given
            UserCreateCommand command =
                    new UserCreateCommand(
                            "test1234",
                            "비밀번호",
                            "김철수",
                            "U123456780",
                            UUID.randomUUID(),
                            null,
                            RequestedRole.HUB_MANAGER);

            given(hubClientPort.existsById(any())).willReturn(false);

            // when & then
            assertThatThrownBy(() -> validator.validateSignUpAffiliation(command))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ClientErrorCode.HUB_NOT_FOUND.message());

            verify(hubClientPort).existsById(any());
            verify(companyClientPort, never()).getCompanyInfo(any());
        }

        @Test
        @DisplayName("업체 유효성 검증 성공")
        void validateSignUpAffiliation_success_with_company() {
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

            CompanyInfo findCompany = new CompanyInfo(command.companyId(), UUID.randomUUID());

            given(companyClientPort.getCompanyInfo(any())).willReturn(findCompany);

            // when & then
            assertThatCode(() -> validator.validateSignUpAffiliation(command))
                    .doesNotThrowAnyException();

            verify(hubClientPort, never()).existsById(any());
            verify(companyClientPort).getCompanyInfo(any());
        }
    }

    @Nested
    @DisplayName("허브관리자가 회원 가입 승인 시 허브 소속 확인")
    class ValidateSignUpCompany {
        @Test
        @DisplayName("마스터 권한인 경우 통과한다,")
        void validateManagerPermission_success_when_master() {
            // given
            UUID adminHubId = UUID.randomUUID();
            Role adminRole = Role.MASTER;

            UserCreateCommand command =
                    new UserCreateCommand(
                            "test1234",
                            "Testtest123!",
                            "김철수",
                            "U123456789",
                            UUID.randomUUID(),
                            UUID.randomUUID(),
                            RequestedRole.COMPANY_MANAGER);

            User user = User.create(command);

            // when & then
            assertThatCode(() -> validator.validateManagerPermission(adminHubId, adminRole, user))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("소속 허브가 같은 경우 통과한다.")
        void validateManagerPermission_success_when_matches_hub() {
            // given
            UUID adminHubId = UUID.randomUUID();
            Role adminRole = Role.HUB_MANAGER;

            UserCreateCommand command =
                    new UserCreateCommand(
                            "test1234",
                            "Testtest123!",
                            "김철수",
                            "U123456789",
                            adminHubId,
                            UUID.randomUUID(),
                            RequestedRole.HUB_MANAGER);

            User user = User.create(command);

            // when & then
            assertThatCode(() -> validator.validateManagerPermission(adminHubId, adminRole, user))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("소속 허브가 다르면 FORBIDDEN 예외가 발생한다.")
        void validateManagerPermission_fail_when_matches_hub() {
            UUID adminHubId = UUID.randomUUID();
            Role adminRole = Role.HUB_MANAGER;

            UserCreateCommand command =
                    new UserCreateCommand(
                            "test1234",
                            "Testtest123!",
                            "김철수",
                            "U123456789",
                            UUID.randomUUID(),
                            UUID.randomUUID(),
                            RequestedRole.HUB_MANAGER);

            User user = User.create(command);

            // when & then
            assertThatThrownBy(
                            () -> validator.validateManagerPermission(adminHubId, adminRole, user))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(CommonErrorCode.FORBIDDEN.message());
        }
    }

    @Nested
    @DisplayName("허브, 업체 실존 여부 검증")
    class ValidateAffiliation {
        @Test
        @DisplayName("허브 검증에 성공한다.")
        void validateAffiliation_success_with_hub() {
            // given
            UserCreateCommand command =
                    new UserCreateCommand(
                            "test1234",
                            "비밀번호",
                            "김철수",
                            "U123456780",
                            UUID.randomUUID(),
                            null,
                            RequestedRole.HUB_MANAGER);

            User user = User.create(command);

            given(hubClientPort.existsById(any())).willReturn(true);

            // when & then
            assertThatCode(() -> validator.validateAffiliation(user)).doesNotThrowAnyException();

            verify(hubClientPort).existsById(any());
            verify(companyClientPort, never()).getCompanyInfo(any());
        }

        @Test
        @DisplayName("허브 검증에 실패하면 HUB_NOT_FOUND 예외가 발생한다.")
        void validateAffiliation_fail_hub_not_found() {
            // given
            UserCreateCommand command =
                    new UserCreateCommand(
                            "test1234",
                            "비밀번호",
                            "김철수",
                            "U123456780",
                            UUID.randomUUID(),
                            null,
                            RequestedRole.HUB_MANAGER);

            User user = User.create(command);

            given(hubClientPort.existsById(any())).willReturn(false);

            // when & then
            assertThatThrownBy(() -> validator.validateAffiliation(user))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ClientErrorCode.HUB_NOT_FOUND.message());

            verify(hubClientPort).existsById(any());
            verify(companyClientPort, never()).getCompanyInfo(any());
        }

        @Test
        @DisplayName("업체 유효성 검증 성공")
        void validateAffiliation_success_with_company() {
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

            User user = User.create(command);

            given(companyClientPort.getCompanyInfo(any())).willReturn(any());

            // when & then
            assertThatCode(() -> validator.validateAffiliation(user)).doesNotThrowAnyException();

            verify(hubClientPort, never()).existsById(any());
            verify(companyClientPort).getCompanyInfo(any());
        }
    }
}
