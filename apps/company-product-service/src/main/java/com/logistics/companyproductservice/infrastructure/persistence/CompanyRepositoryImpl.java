package com.logistics.companyproductservice.infrastructure.persistence;

import com.logistics.companyproductservice.domain.model.Company;
import com.logistics.companyproductservice.domain.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CompanyRepositoryImpl implements CompanyRepository {

    private final CompanyJpaRepository companyJpaRepository;

    @Override
    public Company save(Company company) {
        return companyJpaRepository.save(company);
    }

    @Override
    public Company saveAndFlush(Company company) {
        return companyJpaRepository.saveAndFlush(company);
    }

    @Override
    public boolean existsByName(String name) {
        return companyJpaRepository.existsByNameAndDeletedAtIsNull(name);
    }

    @Override
    public Optional<Company> findByIdAndDeletedAtIsNull(UUID id) {
        return companyJpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public List<Company> findAllByIds(List<UUID> ids) {
        return companyJpaRepository.findAllByIdInAndDeletedAtIsNull(ids);
    }

    @Override
    public Page<Company> search(String name, Pageable pageable) {
        if (name != null && !name.isBlank()) {
            return companyJpaRepository.findAllByNameContainingAndDeletedAtIsNull(name, pageable);
        }
        return companyJpaRepository.findAllByDeletedAtIsNull(pageable);
    }
}