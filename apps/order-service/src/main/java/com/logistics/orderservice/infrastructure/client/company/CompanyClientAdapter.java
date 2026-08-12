package com.logistics.orderservice.infrastructure.client.company;

import com.logistics.orderservice.application.port.CompanyPort;
import com.logistics.orderservice.application.port.ProductPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CompanyClientAdapter implements CompanyPort {

    private final CompanyFeignClient companyFeignClient;

    @Override
    public CompanyInfo getCompanyInfo(UUID companyId) {
        CompanyFeignClient.CompanyResponse response = companyFeignClient
                .getCompany(companyId)
                .getData();

        return new CompanyInfo(
                response.id(),
                response.hubId(),
                response.name(),
                response.address()
        );
    }
}
