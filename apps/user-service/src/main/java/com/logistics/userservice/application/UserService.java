package com.logistics.userservice.application;

import com.logistics.common.error.CommonErrorCode;
import com.logistics.common.exception.BusinessException;
import com.logistics.userservice.application.dto.UserSignUpCommand;
import com.logistics.userservice.domain.User;
import com.logistics.userservice.domain.UserRepository;
import com.logistics.userservice.presentation.exception.UserErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public void createUser(UserSignUpCommand command) {
        User user = User.create(command);
        validateDuplicate(command);

        user.encodePassword(passwordEncoder.encode(user.getPassword()));

        try {
            userRepository.saveAndFlush(user);
            log.debug("[UserService] User 생성 완료");
        } catch (
        DataIntegrityViolationException e) {
            log.error("[UserService] User 생성 실패 : {}", e.getMessage());
            throw new BusinessException(CommonErrorCode.DUPLICATE_RESOURCE);
        }
    }

    private void validateDuplicate(UserSignUpCommand command) {
        List<User> existUsers = userRepository.findByUsernameOrSlackId(command.username(), command.slackId());

        for (User user : existUsers) {
            if (command.username().equals(user.getUsername())) {
                log.debug("[UserService] Username 유효성 체크: {}", command.username());
                throw new BusinessException(UserErrorCode.USER_DUPLICATE_USERNAME);
            }
            if (command.slackId().equals(user.getSlackId())) {
                log.debug("[UserService] Slack ID 유효성 체크: {}", command.slackId());
                throw new BusinessException(UserErrorCode.USER_DUPLICATE_SLACK_ID);
            }
        }
    }
}
