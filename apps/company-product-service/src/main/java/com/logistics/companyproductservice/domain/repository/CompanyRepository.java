package com.logistics.companyproductservice.domain.repository;

import com.logistics.companyproductservice.domain.model.Company;

import java.util.Optional;
import java.util.UUID;

public interface CompanyRepository {

    Company save(Company company);

    boolean existsByName(String name);

    Optional<Company> findById(UUID id);
}