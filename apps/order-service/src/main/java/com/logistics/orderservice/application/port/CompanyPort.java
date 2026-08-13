package com.logistics.orderservice.application.port;

import java.util.UUID;

public interface CompanyPort {

    CompanyInfo getCompanyInfo(UUID companyId);


    record CompanyInfo(
            UUID id,
            UUID hubId,
            String name,
            String address
    ){
    }



}
