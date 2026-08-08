package com.logistics.companyproductservice.infrastructure.persistence;

import com.logistics.companyproductservice.domain.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProductJpaRepository extends JpaRepository<Product, UUID> {
    Optional<Product> findByIdAndDeletedAtIsNull(UUID id);
    Page<Product> findAllByDeletedAtIsNull(Pageable pageable);
    Page<Product> findAllByNameContainingAndDeletedAtIsNull(String name, Pageable pageable);
}