package com.logistics.orderservice.infrastructure.client.company;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(
        name = "company-product-service",
        contextId = "companyFeignClient",
        path = "/internal/v1/companies"
)
public interface CompanyFeignClient {

    @GetMapping("/{companyId}")
    CompanyResponse getCompany(@PathVariable("companyId") UUID companyId);

    record CompanyResponse(
        UUID id,
        UUID hubId,
        String name,
        String address
    ){

    }
}
