package com.logistics.deliveryservice.infrastructure.persistence;

import com.logistics.deliveryservice.domain.model.DeliveryManagerAssignmentCursor;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface DeliveryManagerAssignmentCursorJpaRepository
        extends JpaRepository<DeliveryManagerAssignmentCursor, UUID> {

    @Modifying(flushAutomatically = true)
    @Query(value = """
            insert into p_delivery_manager_assignment_cursors (
                cursor_id,
                assignment_group_key,
                manager_type,
                hub_id,
                created_at,
                updated_at
            ) values (
                :cursorId,
                :assignmentGroupKey,
                :managerType,
                :hubId,
                current_timestamp,
                current_timestamp
            )
            on conflict (assignment_group_key) do nothing
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("cursorId") UUID cursorId,
            @Param("assignmentGroupKey") String assignmentGroupKey,
            @Param("managerType") String managerType,
            @Param("hubId") UUID hubId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select cursor
            from DeliveryManagerAssignmentCursor cursor
            where cursor.assignmentGroupKey = :assignmentGroupKey
            """)
    Optional<DeliveryManagerAssignmentCursor> findByAssignmentGroupKeyForUpdate(
            @Param("assignmentGroupKey") String assignmentGroupKey
    );
}
