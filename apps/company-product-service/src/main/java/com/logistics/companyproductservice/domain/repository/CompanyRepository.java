package com.logistics.companyproductservice.domain.repository;

import com.logistics.companyproductservice.domain.model.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface CompanyRepository {
    Company save(Company company);
    boolean existsByName(String name);
    Optional<Company> findById(UUID id);
    Page<Company> search(String name, Pageable pageable);
}