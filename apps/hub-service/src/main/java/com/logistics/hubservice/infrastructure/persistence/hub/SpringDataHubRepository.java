package com.logistics.hubservice.infrastructure.persistence.hub;

import com.logistics.hubservice.domain.hub.Hub;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataHubRepository extends JpaRepository<Hub, UUID> {

    Optional<Hub> findByIdAndDeletedAtIsNull(UUID id);

    @Query("""
            SELECT h
            FROM Hub h
            WHERE h.deletedAt IS NULL
              AND (
                  :keyword IS NULL
                  OR LOWER(h.name) LIKE CONCAT('%', :keyword, '%')
                  OR LOWER(h.address) LIKE CONCAT('%', :keyword, '%')
              )
            """)
    Page<Hub> search(@Param("keyword") String keyword, Pageable pageable);
}
