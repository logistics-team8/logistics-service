package com.logistics.userservice.application;

import static com.logistics.userservice.application.dto.user.AffiliationType.HUB;

import com.logistics.common.error.CommonErrorCode;
import com.logistics.common.exception.BusinessException;
import com.logistics.userservice.application.dto.admin.*;
import com.logistics.userservice.application.dto.company.CompanyInfo;
import com.logistics.userservice.application.dto.user.UserContext;
import com.logistics.userservice.application.dto.user.UserCreateCommand;
import com.logistics.userservice.application.dto.user.UserUpdateCommand;
import com.logistics.userservice.application.event.UserApprovalEvent;
import com.logistics.userservice.application.event.UserDeletedEvent;
import com.logistics.userservice.application.port.AdminUserQueryRepository;
import com.logistics.userservice.application.port.CompanyClientPort;
import com.logistics.userservice.application.port.HubClientPort;
import com.logistics.userservice.application.validator.UserValidator;
import com.logistics.userservice.domain.RequestedRole;
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
    private final HubClientPort hubClientPort;
    private final CompanyClientPort companyClientPort;
    private final ApplicationEventPublisher applicationEventPublisher;

    // ============================== CRUD ==============================
    /**
     * Admin User 등록 OpenFeign 호출로 회원가입 시 실존 허브 / 업체 1차 검증
     *
     * @param approvedBy
     * @param command
     */
    @Transactional
    public void createUserByAdmin(UserCreateCommand command) {
        validator.validateDuplicate(command);
        User createdUser = User.create(command);

        if (command.affiliationType() == HUB) {
            hubClientPort.existsById(command.hubId());
        } else {
            CompanyInfo companyInfo = companyClientPort.getCompanyInfo(command.companyId());
            createdUser.assignAffiliation(companyInfo.hubId(), companyInfo.companyId());
        }

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
     * 회원가입 요청 승인 실존 허브 / 업체 2차 검증 배송이 아닌 경우 바로 APPROVED(승인) 처리 배송 담당자 생성의 경우 Delivery Service 호출이
     * 필요하므로 상태 값을 PROVISIONING(처리 중)로 처리하여 호출 성공 시 APPROVED 처리 서버 장애 시 회원가입 자체는 완료 -> 스케쥴러를 사용해
     * 일정주기 재시도
     *
     * @param command
     */
    @Transactional
    public void approveUser(AdminApprovalCommand command) {
        User user = findUserById(command.userId());
        validateManagerPermission(command.hubId(), command.role(), user);

        if (user.getCompanyId() == null) {
            hubClientPort.existsById(user.getHubId());
        } else {
            companyClientPort.getCompanyInfo(user.getCompanyId());
        }
        user.approve(command.adminId());

        if (user.getRequestedRole() == RequestedRole.HUB_DELIVERY_MANAGER
                || user.getRequestedRole() == RequestedRole.COMPANY_DELIVERY_MANAGER) {
            RequestedRole requestedRole =
                    user.getCompanyId() == null
                            ? RequestedRole.HUB_DELIVERY_MANAGER
                            : RequestedRole.COMPANY_DELIVERY_MANAGER;
            applicationEventPublisher.publishEvent(
                    new UserApprovalEvent(user.getId(), user.getHubId(), requestedRole));
        }
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
