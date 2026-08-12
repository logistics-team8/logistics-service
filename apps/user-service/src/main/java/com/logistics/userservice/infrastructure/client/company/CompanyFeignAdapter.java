package com.logistics.userservice.infrastructure.client.company;

import com.logistics.common.exception.BusinessException;
import com.logistics.userservice.application.dto.company.CompanyInfo;
import com.logistics.userservice.application.port.CompanyClientPort;
import com.logistics.userservice.error.ClientErrorCode;
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
            if (response == null || !Objects.equals(companyId, response.Id())) {
                log.warn("[ERROR] 업체 ID 불일치 companyId = {}", companyId);
                throw new BusinessException(ClientErrorCode.COMPANY_ID_INVALID);
            }
            log.info("[SUCCESS] 업체 ID 검증 성공 companyId = {}", companyId);
            return response;
        } catch (FeignException.NotFound e) {
            log.warn("[NOT_FOUND] 존재하지 않는 업체 companyId = {}", companyId);
            throw new BusinessException(ClientErrorCode.COMPANY_NOT_FOUND);
        } catch (FeignException e) {
            log.error(
                    "[ERROR] Company-Service 호출 실패 companyId = {}, status = {}, content = {}",
                    companyId,
                    e.status(),
                    e.contentUTF8(),
                    e);
            throw new BusinessException(ClientErrorCode.SERVICE_UNAVAILABLE);
        }
    }
}
