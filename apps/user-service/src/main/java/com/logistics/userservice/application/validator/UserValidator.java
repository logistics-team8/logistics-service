package com.logistics.userservice.application.validator;

import com.logistics.common.exception.BusinessException;
import com.logistics.userservice.application.dto.user.UserCreateCommand;
import com.logistics.userservice.domain.User;
import com.logistics.userservice.domain.UserRepository;
import com.logistics.userservice.error.UserErrorCode;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserValidator {
    private final UserRepository userRepository;

    /**
     * 회원가입 중복 체크 로직 List 형태로 중복인 아이디 또는 Slack ID를 찾고 동일할 시 예외처리
     *
     * @param command
     */
    public void validateDuplicate(UserCreateCommand command) {
        List<User> existUsers =
                userRepository.findByUsernameOrSlackId(command.username(), command.slackId());

        for (User user : existUsers) {
            if (Objects.equals(command.username(), user.getUsername())) {
                throw new BusinessException(UserErrorCode.USER_DUPLICATE_USERNAME);
            }
            if (Objects.equals(command.slackId(), user.getSlackId())) {
                throw new BusinessException(UserErrorCode.USER_DUPLICATE_SLACK_ID);
            }
        }
    }
}
