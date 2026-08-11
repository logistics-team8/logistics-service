package com.logistics.userservice.domain;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import com.logistics.common.exception.BusinessException;
import com.logistics.userservice.application.dto.user.UserCreateCommand;
import com.logistics.userservice.error.AuthErrorCode;
import org.junit.jupiter.api.Test;

class UserEntityTest {
    private User user;

    @Test
    void validateActive_success() {
        // given
        UserCreateCommand command =
                new UserCreateCommand(
                        "test1234",
                        "Testtest123!",
                        "김철수",
                        "U123456789",
                        null,
                        null,
                        RequestedRole.COMPANY_MANAGER,
                        null);

        user = User.create(command);

        // when & then
        assertThatThrownBy(() -> user.validateActive())
                .isInstanceOf(BusinessException.class)
                .hasMessage(AuthErrorCode.PENDING_APPROVAL.message());
    }
}
