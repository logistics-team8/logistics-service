package com.logistics.userservice.domain;

import com.github.f4b6a3.uuid.UuidCreator;
import com.logistics.common.exception.BusinessException;
import com.logistics.userservice.application.dto.user.UserCreateCommand;
import com.logistics.userservice.error.AuthErrorCode;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "p_users")
public class User extends BaseEntity {
    @Id
    @Column(name = "user_id")
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String slackId;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private UserStatus userStatus;

    @Column private UUID hubId;

    @Column private UUID companyId;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private RequestedRole requestedRole;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column private UUID approvedBy;

    @Column private LocalDateTime approvedAt;

    @Column(length = 255)
    private String rejectionReason;

    public static User create(UserCreateCommand command) {
        User user = new User();
        user.username = command.username();
        user.password = command.password();
        user.name = command.name();
        user.slackId = command.slackId();
        user.userStatus = UserStatus.PENDING;
        user.hubId = command.hubId();
        user.companyId = command.companyId();
        user.requestedRole = command.requestedRole();
        return user;
    }

    public static User createByAdmin(UUID approvedBy, UserCreateCommand command) {
        User user = create(command);
        user.approve(approvedBy);
        return user;
    }

    public void encodePassword(String password) {
        this.password = password;
    }

    public void validateActive() {
        if (this.userStatus == UserStatus.PENDING || this.userStatus == UserStatus.PROCESSING) {
            throw new BusinessException(AuthErrorCode.PENDING_APPROVAL);
        }

        if (this.userStatus == UserStatus.REJECTED) {
            throw new BusinessException(AuthErrorCode.APPROVAL_REJECTED);
        }

        if (this.role == null) {
            throw new BusinessException(AuthErrorCode.PENDING_APPROVAL);
        }
    }

    /** 회원 업데이트 */
    public void update(String name, String slackId) {
        if (StringUtils.hasText(name)) {
            this.name = name;
        }
        if (StringUtils.hasText(slackId)) {
            this.slackId = slackId;
        }
    }

    /** 회원 삭제 */
    public void delete(UUID deletedBy) {
        super.delete(deletedBy);
        this.username = this.username + "_" + this.id;
        this.slackId = this.slackId + "_" + this.id;
    }

    /**
     * 허브 / 업체 조회 이후 할당
     *
     * @param hubId
     * @param companyId
     */
    public void assignAffiliation(UUID hubId, UUID companyId) {
        this.hubId = hubId;
        this.companyId = companyId;
    }

    /**
     * 회원 가입 요청 승인, 배송 담당자의 경우 Delivery Service를 호출하기에 서버 장애를 대비하여 PROVISIONING 처리
     *
     * @param approvedBy 승인자 UUID
     */
    public void approve(UUID approvedBy) {
        if (this.requestedRole == RequestedRole.COMPANY_DELIVERY
                || this.requestedRole == RequestedRole.HUB_DELIVERY) {
            this.userStatus = UserStatus.PROCESSING;
        } else {
            this.userStatus = UserStatus.APPROVED;
            this.role = this.requestedRole.toRole();
            this.requestedRole = null;
        }
        this.approvedBy = approvedBy;
        this.approvedAt = LocalDateTime.now();
    }

    /** 배송 담당자 최종 가입 승인 */
    public void completeProvisioning() {
        this.userStatus = UserStatus.APPROVED;
        this.requestedRole = null;
    }

    /**
     * 회원가입 요청 거절
     *
     * @param approvedBy 거부자 UUID
     * @param rejectionReason 거절 사유
     */
    public void reject(UUID approvedBy, String rejectionReason) {
        this.userStatus = UserStatus.REJECTED;
        this.rejectionReason = rejectionReason;
        this.approvedBy = approvedBy;
        this.approvedAt = LocalDateTime.now();
    }

    /**
     * 허브 직원 확인
     *
     * @param hubId 승인자 허브 ID
     * @return 결과 boolean
     */
    public boolean isManagedByHub(UUID hubId) {
        return Objects.equals(this.hubId, hubId);
    }

    /** UUID 삽입 */
    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UuidCreator.getTimeOrderedEpoch();
        }
    }
}
