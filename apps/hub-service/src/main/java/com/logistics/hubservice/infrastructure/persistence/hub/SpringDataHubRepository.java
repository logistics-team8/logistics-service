package com.logistics.hubservice.infrastructure.persistence.hub;

import com.logistics.hubservice.domain.hub.Hub;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataHubRepository extends JpaRepository<Hub, UUID> {

    Optional<Hub> findByIdAndDeletedAtIsNull(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM Hub h WHERE h.id = :id AND h.deletedAt IS NULL")
    Optional<Hub> findByIdAndDeletedAtIsNullForUpdate(@Param("id") UUID id);

    Page<Hub> findAllByDeletedAtIsNull(Pageable pageable);

    @Query("""
            SELECT h
            FROM Hub h
            WHERE h.deletedAt IS NULL
              AND (LOWER(h.name) LIKE CONCAT('%', :keyword, '%')
                  OR LOWER(h.address) LIKE CONCAT('%', :keyword, '%'))
            """)
    Page<Hub> search(@Param("keyword") String keyword, Pageable pageable);
}
