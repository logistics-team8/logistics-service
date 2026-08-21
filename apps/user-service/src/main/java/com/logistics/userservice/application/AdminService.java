package com.logistics.userservice.application;

import com.logistics.common.exception.BusinessException;
import com.logistics.userservice.application.dto.admin.*;
import com.logistics.userservice.application.dto.user.UserContext;
import com.logistics.userservice.application.dto.user.UserCreateCommand;
import com.logistics.userservice.application.dto.user.UserUpdateCommand;
import com.logistics.userservice.application.event.UserApprovalEvent;
import com.logistics.userservice.application.port.AdminUserQueryRepository;
import com.logistics.userservice.application.validator.UserValidator;
import com.logistics.userservice.domain.RequestedRole;
import com.logistics.userservice.domain.User;
import com.logistics.userservice.domain.UserRepository;
import com.logistics.userservice.error.UserErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {
    private final UserValidator validator;
    private final UserService userService;
    private final UserRepository userRepository;
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
        User createdUser = userService.createUser(command);
        createdUser.approve(adminId);

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
    public void updateUser(UserUpdateCommand command) {userService.updateUser(command);}

    /**
     * 회원 정보 삭제
     *
     * @param userId
     */
    @Transactional
    public void deleteUser(UUID userId) { userService.deleteUser(userId); }

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
