package com.logistics.userservice.infrastructure.client.company;

import java.util.UUID;

public record CompanyExistsResponse(
        UUID hubId,
        UUID companyId
) {
}
