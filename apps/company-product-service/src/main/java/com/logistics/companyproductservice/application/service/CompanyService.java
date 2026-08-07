package com.logistics.companyproductservice.application.service;

import com.logistics.companyproductservice.domain.model.Company;
import com.logistics.companyproductservice.domain.repository.CompanyRepository;
import com.logistics.companyproductservice.presentation.dto.request.CompanyCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;

    public Company create(CompanyCreateRequest request) {
        Company company = Company.create(
                request.getName(),
                request.getType(),
                request.getHubId(),
                request.getAddress()
        );

        return companyRepository.save(company);
    }
}