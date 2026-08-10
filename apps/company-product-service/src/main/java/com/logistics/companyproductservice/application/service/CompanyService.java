package com.logistics.companyproductservice.application.service;

import com.logistics.common.error.CommonErrorCode;
import com.logistics.common.exception.BusinessException;
import com.logistics.common.response.PageableUtil;
import com.logistics.common.security.principal.CustomUserDetails;
import com.logistics.companyproductservice.application.dto.CompanyInfo;
import com.logistics.companyproductservice.application.error.CompanyErrorCode;
import com.logistics.companyproductservice.domain.model.Company;
import com.logistics.companyproductservice.domain.repository.CompanyRepository;
import com.logistics.companyproductservice.presentation.dto.request.CompanyCreateRequest;
import com.logistics.companyproductservice.presentation.dto.request.CompanyUpdateRequest;
import com.logistics.companyproductservice.presentation.dto.response.CompanyResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyService {

    private static final Set<String> ALLOWED_SORT = Set.of("createdAt", "name");

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

    public Company getCompany(UUID id) {
        return companyRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
    }

    public CompanyInfo getCompanyInfo(UUID companyId) {
        Company company = companyRepository.findByIdAndDeletedAtIsNull(companyId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
        return CompanyInfo.from(company);
    }

    public List<CompanyInfo> getCompanyInfos(List<UUID> companyIds) {
        List<Company> companies = companyRepository.findAllByIds(companyIds);
        if (companies.size() != companyIds.size()) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND);
        }
        return companies.stream().map(CompanyInfo::from).toList();
    }

    @Transactional
    public Company update(UUID id, CompanyUpdateRequest request, CustomUserDetails userDetails) {
        Company company = companyRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

        validateOwnership(company, userDetails);

        if (request.getName() != null
                && !request.getName().equals(company.getName())
                && companyRepository.existsByName(request.getName())) {
            throw new BusinessException(CommonErrorCode.DUPLICATE_RESOURCE);
        }

        company.update(request.getName(), request.getAddress());
        return company;
    }

    @Transactional
    public void delete(UUID id, CustomUserDetails userDetails) {
        Company company = companyRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

        validateOwnership(company, userDetails);
        company.delete(userDetails.getId());
    }

    public Page<CompanyResponse> search(String name, Pageable pageable) {
        Pageable normalized = PageableUtil.normalize(pageable, ALLOWED_SORT);
        return companyRepository.search(name, normalized).map(CompanyResponse::from);
    }

    private void validateOwnership(Company company, CustomUserDetails userDetails) {
        boolean isMaster = "MASTER".equals(userDetails.getRole());
        boolean isOwnCompany = company.getId().equals(userDetails.getCompanyId());
        if (!isMaster && !isOwnCompany) {
            throw new BusinessException(CompanyErrorCode.NOT_OWNED_COMPANY);
        }
    }
}