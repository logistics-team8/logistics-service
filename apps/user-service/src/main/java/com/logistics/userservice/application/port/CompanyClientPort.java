package com.logistics.userservice.application.port;

import com.logistics.userservice.application.dto.company.CompanyInfo;
import java.util.UUID;

public interface CompanyClientPort {
    CompanyInfo getCompanyInfo(UUID companyId);
}
