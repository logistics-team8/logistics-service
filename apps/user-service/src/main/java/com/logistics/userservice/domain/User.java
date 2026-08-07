package com.logistics.userservice.domain;

import com.github.f4b6a3.uuid.UuidCreator;
import com.logistics.common.exception.BusinessException;
import com.logistics.userservice.application.dto.UserSignUpCommand;
import com.logistics.userservice.error.AuthErrorCode;
import com.logistics.userservice.infrastructure.config.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
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

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column private UUID approvedBy;

    @Column private LocalDateTime approvedAt;

    @Column(length = 255) private String rejectionReason;

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

    public void validateActive() {
        if (this.userStatus == UserStatus.PENDING) {
            throw new BusinessException(AuthErrorCode.PENDING_APPROVAL);
        }
        if (this.userStatus == UserStatus.REJECTED) {
            throw new BusinessException(AuthErrorCode.APPROVAL_REJECTED);
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

    /** UUID 삽입 */
    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UuidCreator.getTimeOrderedEpoch();
        }
    }
}
