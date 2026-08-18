package com.logistics.orderservice.infrastructure.client.company;

import com.logistics.common.exception.BusinessException;
import com.logistics.orderservice.application.port.CompanyPort;
import com.logistics.orderservice.error.OrderErrorCode;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CompanyClientAdapter implements CompanyPort {

    private final CompanyFeignClient companyFeignClient;

    @Override
    public CompanyInfo getCompanyInfo(UUID companyId) {
        try {
            CompanyFeignClient.CompanyResponse response = companyFeignClient.getCompany(companyId).getData();

            if (response == null) {
                throw new BusinessException(
                        OrderErrorCode.RECEIVER_COMPANY_NOT_FOUND
                );
            }

            return new CompanyInfo(
                    response.id(),
                    response.hubId(),
                    response.name(),
                    response.address()
            );
        } catch (FeignException.NotFound exception) {
            throw new BusinessException(
                    OrderErrorCode.RECEIVER_COMPANY_NOT_FOUND
            );
        }
    }
}
