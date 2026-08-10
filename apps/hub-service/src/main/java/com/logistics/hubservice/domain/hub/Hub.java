package com.logistics.hubservice.domain.hub;

import com.logistics.hubservice.domain.common.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import org.springframework.data.domain.Persistable;

@Getter
@Entity
@Table(name = "p_hubs")
public class Hub extends BaseEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    private String name;
    private String address;
    @Column(precision = 9, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;
    private LocalDateTime deletedAt;
    private UUID deletedBy;

    @Transient
    private boolean newEntity = true;

    protected Hub() {
    }

    private Hub(String name, String address, BigDecimal latitude, BigDecimal longitude) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    private Hub(
            UUID id,
            String name,
            String address,
            BigDecimal latitude,
            BigDecimal longitude) {
        this(name, address, latitude, longitude);
        if (id == null) {
            throw new IllegalArgumentException("Hub ID는 필수입니다.");
        }
        this.id = id;
    }

    public static Hub create(String name, String address, BigDecimal latitude, BigDecimal longitude) {
        return new Hub(name, address, latitude, longitude);
    }

    public static Hub createDefault(
            UUID id,
            String name,
            String address,
            BigDecimal latitude,
            BigDecimal longitude) {
        Hub hub = new Hub(id, name, address, latitude, longitude);
        hub.initializeSystemAudit();
        return hub;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    @PostPersist
    @PostLoad
    private void markNotNew() {
        newEntity = false;
    }

    public void update(String name, String address, BigDecimal latitude, BigDecimal longitude) {
        ensureActive();
        if (name != null) {
            this.name = name;
        }
        if (address != null) {
            this.address = address;
        }
        if (latitude != null) {
            this.latitude = latitude;
        }
        if (longitude != null) {
            this.longitude = longitude;
        }
    }

    public void delete(UUID deletedBy) {
        ensureActive();
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedBy;
    }

    private void ensureActive() {
        if (deletedAt != null) {
            throw new IllegalStateException("삭제된 Hub는 변경할 수 없습니다.");
        }
    }

}
