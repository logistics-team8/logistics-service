package com.logistics.userservice.application;

import com.logistics.common.error.CommonErrorCode;
import com.logistics.common.exception.BusinessException;
import com.logistics.userservice.application.dto.*;
import com.logistics.userservice.domain.User;
import com.logistics.userservice.domain.UserRepository;
import com.logistics.userservice.presentation.exception.UserErrorCode;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * User 회원가입
     *
     * @param command
     */
    @Transactional
    public void createUser(UserSignUpCommand command) {
        User user = User.create(command);
        validateDuplicate(command);

        user.encodePassword(passwordEncoder.encode(user.getPassword()));

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(CommonErrorCode.DUPLICATE_RESOURCE);
        }
    }

    /**
     * 회원가입 중복 체크 로직 List 형태로 중복인 아이디 또는 Slack ID를 찾고 동일할 시 예외처리
     *
     * @param command
     */
    private void validateDuplicate(UserSignUpCommand command) {
        List<User> existUsers =
                userRepository.findByUsernameOrSlackId(command.username(), command.slackId());

        for (User user : existUsers) {
            if (command.username().equals(user.getUsername())) {
                throw new BusinessException(UserErrorCode.USER_DUPLICATE_USERNAME);
            }
            if (command.slackId().equals(user.getSlackId())) {
                throw new BusinessException(UserErrorCode.USER_DUPLICATE_SLACK_ID);
            }
        }
    }

    public UserInfo getUserInfo(UUID userId) {
        return UserInfo.from(
                userRepository
                        .findByIdAndDeletedAtIsNull(userId)
                        .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND)));
    }

    @Transactional(readOnly = true)
    public UserRoleInfo getUserRole(UUID userId) {
        return new UserRoleInfo(
                userRepository
                        .findRoleByIdDeletedAtIsNull(userId)
                        .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND)));
    }

    @Transactional(readOnly = true)
    public UserSlackInfo getUserSlackId(UUID userId) {
        return new UserSlackInfo(
                userRepository
                        .findSlackIdByIdDeletedAtIsNull(userId)
                        .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND)));
    }

    @Transactional
    public void deleteUser(UUID userId) {

    }

    @Transactional
    public void updateUser(UserUpdateCommand command) {}
}
