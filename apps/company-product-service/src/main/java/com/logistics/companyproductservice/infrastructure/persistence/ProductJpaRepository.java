package com.logistics.companyproductservice.infrastructure.persistence;

import com.logistics.companyproductservice.domain.model.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductJpaRepository extends JpaRepository<Product, UUID> {
    Optional<Product> findByIdAndDeletedAtIsNull(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id and p.deletedAt is null")
    Optional<Product> findByIdForUpdate(@Param("id") UUID id);

    Page<Product> findAllByDeletedAtIsNull(Pageable pageable);
    Page<Product> findAllByNameContainingAndDeletedAtIsNull(String name, Pageable pageable);
    Page<Product> findAllByHubIdAndDeletedAtIsNull(UUID hubId, Pageable pageable);
    Page<Product> findAllByNameContainingAndHubIdAndDeletedAtIsNull(String name, UUID hubId, Pageable pageable);
    List<Product> findAllByIdInAndDeletedAtIsNull(List<UUID> ids);
}