package com.logistics.userservice.infrastructure.client.company;

import com.logistics.common.response.ApiResponse;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "company-product-service", path = "/internal/v1/companies")
public interface CompanyFeignClient {
    @GetMapping("/{companyId}")
    ApiResponse<CompanyResponse> getCompanyInfo(@PathVariable("companyId") UUID companyId);
}
