package com.logistics.companyproductservice.infrastructure.persistence;

import com.logistics.companyproductservice.domain.model.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyJpaRepository extends JpaRepository<Company, UUID> {
    boolean existsByName(String name);
    boolean existsByNameAndDeletedAtIsNull(String name);
    Optional<Company> findByIdAndDeletedAtIsNull(UUID id);
    List<Company> findAllByIdInAndDeletedAtIsNull(List<UUID> ids);
    Page<Company> findAllByDeletedAtIsNull(Pageable pageable);
    Page<Company> findAllByNameContainingAndDeletedAtIsNull(String name, Pageable pageable);
}