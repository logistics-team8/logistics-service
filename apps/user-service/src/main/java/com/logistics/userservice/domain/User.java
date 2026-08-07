package com.logistics.userservice.domain;

import com.github.f4b6a3.uuid.UuidCreator;
import com.logistics.common.exception.BusinessException;
import com.logistics.userservice.application.dto.UserSignUpCommand;
import com.logistics.userservice.infrastructure.config.BaseEntity;
import com.logistics.userservice.presentation.exception.AuthErrorCode;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;

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

    @Column
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column private UUID approvedBy;

    @Column private LocalDateTime approvedAt;

    @Column private String rejectionReason;

    public static User create(UserSignUpCommand command) {
        User user = new User();
        user.username = command.username();
        user.password = command.password();
        user.name = command.name();
        user.slackId = command.slackId();
        user.userStatus = UserStatus.PENDING;
        user.hubId = command.hub_id();
        user.companyId = command.company_id();
        user.role = command.role();
        return user;
    }

    public void encodePassword(String password) {
        this.password = password;
    }

    /** 사용자 검증 */
    public void validateActive() {
        if (this.userStatus == UserStatus.PENDING) {
            throw new BusinessException(AuthErrorCode.PENDING_APPROVAL);
        }
        if (this.userStatus == UserStatus.REJECTED) {
            throw new BusinessException(AuthErrorCode.APPROVAL_REJECTED);
        }
    }

    /** UUID v7 삽입 */
    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UuidCreator.getTimeOrderedEpoch();
        }
    }
}
