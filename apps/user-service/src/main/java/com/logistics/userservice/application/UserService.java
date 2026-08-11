package com.logistics.userservice.application;

import static com.logistics.userservice.application.dto.user.AffiliationType.HUB;

import com.logistics.common.error.CommonErrorCode;
import com.logistics.common.exception.BusinessException;
import com.logistics.userservice.application.dto.company.CompanyInfo;
import com.logistics.userservice.application.dto.user.UserCreateCommand;
import com.logistics.userservice.application.dto.user.UserInfo;
import com.logistics.userservice.application.dto.user.UserRoleInfo;
import com.logistics.userservice.application.dto.user.UserSlackInfo;
import com.logistics.userservice.application.dto.user.UserUpdateCommand;
import com.logistics.userservice.application.event.UserDeletedEvent;
import com.logistics.userservice.application.port.CompanyClientPort;
import com.logistics.userservice.application.port.HubClientPort;
import com.logistics.userservice.application.validator.UserValidator;
import com.logistics.userservice.domain.User;
import com.logistics.userservice.domain.UserRepository;
import com.logistics.userservice.error.UserErrorCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {
    private final UserValidator validator;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final HubClientPort hubClientPort;
    private final CompanyClientPort companyClientPort;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * User 회원가입 OpenFeign 호출로 회원가입 시 실존 허브 / 업체 1차 검증
     *
     * @param command
     */
    @Transactional
    public void createUser(UserCreateCommand command) {
        validator.validateDuplicate(command);
        User user = User.create(command);

        if (command.affiliationType() == HUB) {
            hubClientPort.existsById(command.hubId());
        } else {
            CompanyInfo companyInfo = companyClientPort.getCompanyInfo(command.companyId());
            user.assignAffiliation(companyInfo.hubId(), companyInfo.companyId());
        }

        user.encodePassword(passwordEncoder.encode(user.getPassword()));

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(CommonErrorCode.DUPLICATE_RESOURCE);
        }
    }

    /**
     * 회원 정보 조회
     *
     * @param userId
     * @return UserInfo DTO 객체
     */
    public UserInfo getUserInfo(UUID userId) {
        return UserInfo.from(findUserById(userId));
    }

    /**
     * 회원 탈퇴 탈퇴 처리 시 Redis에서 사용자 인증 정보 삭제
     *
     * @param userId
     */
    @Transactional
    public void deleteUser(UUID userId) {
        User deletedUser = findUserById(userId);
        deletedUser.delete(deletedUser.getId());

        // Redis 인증 정보 삭제는 이벤트 처리
        applicationEventPublisher.publishEvent(new UserDeletedEvent(userId));
    }

    /**
     * 회원 정보 수정 SlackId는 유니크 제약 조건이 걸려있으므로 Flush 하여 예외처리
     *
     * @param command
     */
    @Transactional
    public void updateUser(UserUpdateCommand command) {
        User updatedUser = findUserById(command.userId());
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

    // ============================== Helper Method ====================================

    /**
     * 회원 조회 헬퍼 메서드
     *
     * @param userId
     * @return
     */
    private User findUserById(UUID userId) {
        return userRepository
                .findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
    }
}
