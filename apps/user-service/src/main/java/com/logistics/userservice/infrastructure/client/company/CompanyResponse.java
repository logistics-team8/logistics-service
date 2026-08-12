package com.logistics.userservice.infrastructure.client.company;

import java.util.UUID;

public record CompanyResponse(UUID id, UUID hubId, String name, String address) {}
