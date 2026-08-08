package com.logistics.companyproductservice.application.service;

import com.logistics.common.error.CommonErrorCode;
import com.logistics.common.exception.BusinessException;
import com.logistics.companyproductservice.domain.model.Company;
import com.logistics.companyproductservice.domain.repository.CompanyRepository;
import com.logistics.companyproductservice.presentation.dto.request.CompanyCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;

    @Transactional
    public Company create(CompanyCreateRequest request) {
        if (companyRepository.existsByName(request.getName())) {
            throw new BusinessException(CommonErrorCode.DUPLICATE_RESOURCE);
        }

        //hub-service에 request.getHubId()가 실제 존재하는 Hub인지 검증 필요
        Company company = Company.create(
                request.getName(),
                request.getType(),
                request.getHubId(),
                request.getAddress()
        );

        return companyRepository.save(company);
    }
    @Transactional(readOnly = true)
    public Company getCompany(UUID id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
    }
}