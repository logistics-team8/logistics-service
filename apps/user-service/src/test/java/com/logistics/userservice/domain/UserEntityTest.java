package com.logistics.userservice.domain;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import com.logistics.common.exception.BusinessException;
import com.logistics.userservice.application.dto.UserSignUpCommand;
import com.logistics.userservice.presentation.exception.AuthErrorCode;
import org.junit.jupiter.api.Test;

class UserEntityTest {
    private User user;

    @Test
    void validateActive_success() {
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

        user = User.create(command);

        // when & then
        assertThatThrownBy(() -> user.validateActive())
                .isInstanceOf(BusinessException.class)
                .hasMessage(AuthErrorCode.PENDING_APPROVAL.message());
    }
}
