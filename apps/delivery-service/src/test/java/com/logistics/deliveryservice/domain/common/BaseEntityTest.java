package com.logistics.deliveryservice.domain.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BaseEntityTest {

    @Test
    void marksEntityAsDeletedWithActorAndTime() {
        TestEntity entity = new TestEntity();
        UUID deletedBy = UUID.fromString("7f8bfb51-5c00-42a8-a711-6d4c0b829f95");
        LocalDateTime beforeDeletion = LocalDateTime.now();

        entity.delete(deletedBy);

        assertThat(entity.isDeleted()).isTrue();
        assertThat(entity.getDeletedBy()).isEqualTo(deletedBy);
        assertThat(entity.getDeletedAt()).isAfterOrEqualTo(beforeDeletion);
        assertThat(entity.getDeletedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    private static final class TestEntity extends BaseEntity {

        private void delete(UUID deletedBy) {
            markDeleted(deletedBy);
        }
    }
}
