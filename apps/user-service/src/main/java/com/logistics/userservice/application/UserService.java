package com.logistics.userservice.application;

import com.logistics.common.error.CommonErrorCode;
import com.logistics.common.exception.BusinessException;
import com.logistics.userservice.application.dto.*;
import com.logistics.userservice.domain.User;
import com.logistics.userservice.domain.UserRepository;
import com.logistics.userservice.domain.redis.RefreshTokenRepository;
import com.logistics.userservice.domain.redis.RoleCacheRepository;
import com.logistics.userservice.error.UserErrorCode;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
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
    private final RefreshTokenRepository refreshTokenRepository;
    private final RoleCacheRepository roleCacheRepository;

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

    /**
     * 회원 정보 조회
     *
     * @param userId
     * @return UserInfo DTO 객체
     */
    public UserInfo getUserInfo(UUID userId) {
        return UserInfo.from(
                userRepository
                        .findByIdAndDeletedAtIsNull(userId)
                        .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND)));
    }

    /**
     * 회원 탈퇴 탈퇴 처리 시 Redis에서 사용자 인증 정보 삭제
     *
     * @param userId
     */
    @Transactional
    public void deleteUser(UUID userId) {
        User deletedUser =
                userRepository
                        .findByIdAndDeletedAtIsNull(userId)
                        .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
        deletedUser.delete(deletedUser.getId());

        try {
            // 토큰 재발급 시 DB를 조회하므로 탈퇴 프로세스가 진행되도록 적용
            refreshTokenRepository.delete(userId);
            roleCacheRepository.delete(userId);
        } catch (DataAccessException e) {
            log.warn("[FAIL] 회원 탈퇴 후 Redis 인증 정보 삭제 실패 userId = {}", userId, e);
        }
    }

    /**
     * 회원 정보 수정 SlackId는 유니크 제약 조건이 걸려있으므로 Flush 하여 예외처리
     *
     * @param command
     */
    @Transactional
    public void updateUser(UserUpdateCommand command) {
        User updatedUser =
                userRepository
                        .findByIdAndDeletedAtIsNull(command.userId())
                        .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
        updatedUser.update(command.name(), command.slackId());

        try {
            userRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(UserErrorCode.USER_DUPLICATE_SLACK_ID);
        }
    }

    /**
     * 회원 Role 조회(내부통신 전용)
     *
     * @param userId
     * @return UserRoleInfo
     */
    public UserRoleInfo getUserRole(UUID userId) {
        return new UserRoleInfo(
                userRepository
                        .findRoleByIdDeletedAtIsNull(userId)
                        .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND)));
    }

    /**
     * 회원 Slack ID 조회(내부통신 전용)
     *
     * @param userId
     * @return UserSlackInfo
     */
    public UserSlackInfo getUserSlackId(UUID userId) {
        return new UserSlackInfo(
                userRepository
                        .findSlackIdByIdDeletedAtIsNull(userId)
                        .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND)));
    }
}
