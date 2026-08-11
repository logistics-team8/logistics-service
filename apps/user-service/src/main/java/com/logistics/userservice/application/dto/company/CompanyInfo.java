package com.logistics.userservice.application.dto.company;

import java.util.UUID;

public record CompanyInfo(UUID Id, UUID hubId, String name, String address) {}
