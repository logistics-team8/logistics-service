package com.logistics.userservice.application;

import com.logistics.common.error.CommonErrorCode;
import com.logistics.common.exception.BusinessException;
import com.logistics.userservice.application.dto.admin.*;
import com.logistics.userservice.application.dto.user.UserContext;
import com.logistics.userservice.application.dto.user.UserCreateCommand;
import com.logistics.userservice.application.dto.user.UserUpdateCommand;
import com.logistics.userservice.application.event.UserDeletedEvent;
import com.logistics.userservice.application.port.AdminUserQueryRepository;
import com.logistics.userservice.application.validator.UserValidator;
import com.logistics.userservice.domain.Role;
import com.logistics.userservice.domain.User;
import com.logistics.userservice.domain.UserRepository;
import com.logistics.userservice.error.UserErrorCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {
    private final UserValidator validator;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminUserQueryRepository adminUserQueryRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    // ============================== CRUD ==============================
    /**
     * Admin User 등록
     *
     * @param approvedBy
     * @param command
     */
    @Transactional
    public void createUserByAdmin(UUID approvedBy, UserCreateCommand command) {
        User createdUser = User.createByAdmin(approvedBy, command);
        validator.validateDuplicate(command);

        createdUser.encodePassword(passwordEncoder.encode(createdUser.getPassword()));

        try {
            userRepository.saveAndFlush(createdUser);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(CommonErrorCode.DUPLICATE_RESOURCE);
        }
    }

    /**
     * 회원 목록 검색
     *
     * @param userContext
     * @param searchUsersQuery
     * @param pageable
     * @return
     */
    public Page<AdminUserInfo> getUsersInfo(
            UserContext userContext, SearchUsersQuery searchUsersQuery, Pageable pageable) {
        return adminUserQueryRepository
                .searchUsers(userContext, searchUsersQuery, pageable)
                .map(AdminUserInfo::from);
    }

    /**
     * 회원 정보 조회
     *
     * @param userId
     * @return
     */
    public AdminUserInfo getUserInfo(UUID userId) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
        return AdminUserInfo.from(user);
    }

    /**
     * 회원 정보 업데이트
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
     * 회원 정보 삭제
     *
     * @param userId
     */
    @Transactional
    public void deleteUser(UUID userId) {
        User deletedUser = findUserById(userId);
        deletedUser.delete(userId);

        // Redis 인증 정보 삭제는 이벤트 처리
        applicationEventPublisher.publishEvent(new UserDeletedEvent(userId));
    }

    // ============================== Approval ==============================

    /**
     * 회원가입 요청 승인
     *
     * @param command
     */
    @Transactional
    public void approveUser(AdminApprovalCommand command) {
        User user = findUserById(command.userId());
        validateManagerPermission(command.hubId(), command.role(), user);

        user.approve(command.adminId());
    }

    /**
     * 회원가입 요청 거절
     *
     * @param command
     */
    @Transactional
    public void rejectUser(AdminRejectCommand command) {
        User user = findUserById(command.userId());
        validateManagerPermission(command.hubId(), command.role(), user);

        user.reject(command.adminId(), command.reason());
    }

    /**
     * 회원가입 요청 목록 검색
     *
     * @param userContext
     * @param searchUsersQuery
     * @param pageable
     * @return
     */
    public Page<UserApprovalInfo> getPendingUsers(
            UserContext userContext, SearchUsersQuery searchUsersQuery, Pageable pageable) {
        return adminUserQueryRepository
                .searchUsers(userContext, searchUsersQuery, pageable)
                .map(UserApprovalInfo::from);
    }

    // ============================== Helper Method ====================================
    private User findUserById(UUID userId) {
        return userRepository
                .findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
    }

    /**
     * 허브 관리자의 경우 본인 허브 유무 체크
     *
     * @param adminHubId
     * @param adminRole
     * @param user
     */
    private void validateManagerPermission(UUID adminHubId, Role adminRole, User user) {
        if (Role.HUB_MANAGER.equals(adminRole)) {
            if (!user.isManagedByHub(adminHubId)) {
                throw new BusinessException(CommonErrorCode.FORBIDDEN);
            }
        }
    }
}
