package com.logistics.userservice.infrastructure.client.company;

import com.logistics.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "company-product-service", path = "/internal/v1/companies")
public interface CompanyFeignClient {
    ApiResponse<CompanyExistsResponse> checkCompanyExists(@PathVariable("companyId") UUID companyId);
}
