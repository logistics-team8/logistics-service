package com.logistics.deliveryservice.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.logistics.deliveryservice.domain.model.DeliveryManagerAssignmentCursor;
import com.logistics.deliveryservice.domain.model.DeliveryManagerAssignmentGroup;
import jakarta.persistence.LockModeType;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
class DeliveryManagerAssignmentCursorRepositoryAdapterTest {

    @Mock
    private DeliveryManagerAssignmentCursorJpaRepository jpaRepository;

    @InjectMocks
    private DeliveryManagerAssignmentCursorRepositoryAdapter adapter;

    @Test
    void insertsCursorIfAbsentAndReturnsLockedCursor() {
        DeliveryManagerAssignmentGroup group = DeliveryManagerAssignmentGroup.hubDelivery();
        DeliveryManagerAssignmentCursor lockedCursor =
                DeliveryManagerAssignmentCursor.create(group);
        when(jpaRepository.findByAssignmentGroupKeyForUpdate("HUB_DELIVERY:GLOBAL"))
                .thenReturn(Optional.of(lockedCursor));

        DeliveryManagerAssignmentCursor result = adapter.acquireForUpdate(group);

        assertThat(result).isSameAs(lockedCursor);
        verify(jpaRepository).insertIfAbsent(
                any(UUID.class),
                eq("HUB_DELIVERY:GLOBAL"),
                eq("HUB_DELIVERY"),
                eq(null)
        );
        verify(jpaRepository).findByAssignmentGroupKeyForUpdate("HUB_DELIVERY:GLOBAL");
    }

    @Test
    void failsWhenCursorCannotBeReadAfterInsert() {
        DeliveryManagerAssignmentGroup group = DeliveryManagerAssignmentGroup.hubDelivery();
        when(jpaRepository.findByAssignmentGroupKeyForUpdate("HUB_DELIVERY:GLOBAL"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.acquireForUpdate(group))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("배정 그룹 Cursor를 생성하거나 조회할 수 없습니다.");
    }

    @Test
    void usesPostgresUpsertAndPessimisticWriteLock() throws NoSuchMethodException {
        Method insertMethod = DeliveryManagerAssignmentCursorJpaRepository.class
                .getDeclaredMethod(
                        "insertIfAbsent",
                        UUID.class,
                        String.class,
                        String.class,
                        UUID.class
                );
        Method lockMethod = DeliveryManagerAssignmentCursorJpaRepository.class
                .getDeclaredMethod("findByAssignmentGroupKeyForUpdate", String.class);
        Method adapterMethod = DeliveryManagerAssignmentCursorRepositoryAdapter.class
                .getDeclaredMethod(
                        "acquireForUpdate",
                        DeliveryManagerAssignmentGroup.class
                );

        Query insertQuery = insertMethod.getAnnotation(Query.class);
        Lock lock = lockMethod.getAnnotation(Lock.class);
        Transactional transactional = adapterMethod.getAnnotation(Transactional.class);

        assertThat(insertQuery.nativeQuery()).isTrue();
        assertThat(insertQuery.value()).contains("on conflict (assignment_group_key) do nothing");
        assertThat(lock.value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
        assertThat(transactional.propagation()).isEqualTo(Propagation.MANDATORY);
    }
}
