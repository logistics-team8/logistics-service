package com.logistics.userservice.application.port;

import java.util.UUID;

public interface CompanyClientPort {
    boolean existsById(UUID hubId, UUID companyId);
}
