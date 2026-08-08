package com.logistics.companyproductservice.domain.repository;

import com.logistics.companyproductservice.domain.model.Company;

public interface CompanyRepository {

    Company save(Company company);

    boolean existsByName(String name);
}