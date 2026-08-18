package com.logistics.userservice.application;

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
import com.logistics.userservice.application.validator.UserValidator;
import com.logistics.userservice.domain.RequestedRole;
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
     * Admin User 등록 OpenFeign 호출로 회원가입 시 실존 허브 / 업체 검증 후 회원가입 승인 진행
     *
     * @param adminId
     * @param command
     */
    @Transactional
    public void createUserByAdmin(UUID adminId, UserCreateCommand command) {
        validator.validateDuplicate(command);

        // 허브, 업체 존재 여부 검증 - 업체의 경우 CompanyInfo(hubId, CompanyId) 반환
        CompanyInfo companyInfo = validator.validateSignUpAffiliation(command);
        User createdUser = User.create(command);

        if (companyInfo != null) {
            createdUser.assignAffiliation(companyInfo.hubId(), companyInfo.companyId());
        }

        createdUser.encodePassword(passwordEncoder.encode(createdUser.getPassword()));
        createdUser.approve(adminId);

        try {
            userRepository.saveAndFlush(createdUser);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(CommonErrorCode.DUPLICATE_RESOURCE);
        }

        if (createdUser.isDelivery()) {
            RequestedRole requestedRole = createdUser.getRequestedRole();

            // 배송 담당자 가입 승인 시 배송 담당자 생성 이벤트 발행
            applicationEventPublisher.publishEvent(
                    new UserApprovalEvent(
                            createdUser.getId(), createdUser.getHubId(), requestedRole));
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
     * 회원가입 요청 승인 실존 허브 / 업체 2차 검증 배송이 아닌 경우 바로 APPROVED(승인) 배송 담당자 생성의 경우 Delivery Service 호출이
     * 필요하므로 상태 값을 처리 중 상태로 한 뒤, 호출 성공 시 APPROVED(승인) 처리 서버 장애 시 회원가입 자체는 완료 -> 스케쥴러를 사용해 일정 주기 재시도
     *
     * @param command
     */
    @Transactional
    public void approveUser(AdminApprovalCommand command) {
        User user = findUserById(command.userId());

        // 허브 관리자의 경우 본인 담당 허브만 관리 가능
        validator.validateManagerPermission(command.hubId(), command.role(), user);
        validator.validateAffiliation(user);

        user.approve(command.adminId());

        if (user.isDelivery()) {
            RequestedRole requestedRole = user.getRequestedRole();

            // 배송 담당자 가입 승인 시 배송 담당자 생성 이벤트 발행
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
        validator.validateManagerPermission(command.hubId(), command.role(), user);

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
                .searchPendingUsers(userContext, searchUsersQuery, pageable)
                .map(UserApprovalInfo::from);
    }

    // ============================== Helper Method ====================================
    /**
     * 단일 사용자 검색
     *
     * @param userId 검색할 사용자 PK
     * @return User Entity
     */
    private User findUserById(UUID userId) {
        return userRepository
                .findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
    }
}
