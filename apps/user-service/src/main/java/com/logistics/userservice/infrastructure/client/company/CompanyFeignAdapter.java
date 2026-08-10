package com.logistics.userservice.infrastructure.client.company;

import com.logistics.common.error.CommonErrorCode;
import com.logistics.common.exception.BusinessException;
import com.logistics.userservice.application.port.CompanyClientPort;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompanyFeignAdapter implements CompanyClientPort {
    private final CompanyFeignClient companyFeignClient;

    @Override
    public boolean existsById(UUID hubId, UUID companyId) {
        try {
            CompanyExistsResponse response =
                    companyFeignClient.checkCompanyExists(companyId).getData();
            if (response == null) {
                throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR);
            }

            log.info("[SUCCESS] CompanyService 호출 성공 companyId = {}", companyId);
            return Objects.equals(hubId, response.hubId())
                    && Objects.equals(companyId, response.companyId());
        } catch (FeignException.NotFound e) {
            log.info("[SUCCESS] CompanyService 존재하지 않는 업체 companyId = {}", e.getMessage());
            return false;
        } catch (FeignException e) {
            log.error("[ERROR] CompanyService 호출 실패 companyId = {}", companyId, e);
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
