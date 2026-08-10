package com.logistics.companyproductservice.domain.model;

import com.logistics.common.error.CommonErrorCode;
import com.logistics.common.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Getter
@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseEntity {

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @CreatedBy
    @Column(updatable = false)
    private UUID createdBy;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    @LastModifiedBy
    @Column
    private UUID updatedBy;

    @Column
    private Instant deletedAt;

    @Column
    private UUID deletedBy;

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    public void delete(UUID deletedBy) {
        if (this.deletedAt != null) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND);
        }
        this.deletedAt = Instant.now();
        this.deletedBy = deletedBy;
    }
}