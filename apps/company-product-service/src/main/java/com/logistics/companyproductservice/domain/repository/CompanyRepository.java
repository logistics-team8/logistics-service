package com.logistics.companyproductservice.domain.repository;

import com.logistics.companyproductservice.domain.model.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyRepository {
    Company save(Company company);
    Company saveAndFlush(Company company);
    boolean existsByName(String name);
    Optional<Company> findByIdAndDeletedAtIsNull(UUID id);
    List<Company> findAllByIds(List<UUID> ids);
    Page<Company> search(String name, Pageable pageable);
}