package com.logistics.userservice.infrastructure.client.company;

import com.logistics.common.error.CommonErrorCode;
import com.logistics.common.exception.BusinessException;
import com.logistics.userservice.application.dto.company.CompanyInfo;
import com.logistics.userservice.application.port.CompanyClientPort;
import feign.FeignException;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompanyFeignAdapter implements CompanyClientPort {
    private final CompanyFeignClient companyFeignClient;

    @Override
    public CompanyInfo getCompanyInfo(UUID companyId) {
        try {
            CompanyInfo response = companyFeignClient.getCompanyInfo(companyId).getData();
            if (response == null || !Objects.equals(companyId, response.companyId())) {
                log.error("[ERROR] 업체 ID 불일치 companyId = {}", companyId);
                throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR);
            }
            return response;
        } catch (FeignException.NotFound e) {
            log.warn("[NOT_FOUND] 존재하지 않는 업체 companyId = {}", companyId);
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND);
        } catch (FeignException e) {
            log.error("[ERROR] Company-Service 호출 실패 companyId = {}", companyId, e);
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
