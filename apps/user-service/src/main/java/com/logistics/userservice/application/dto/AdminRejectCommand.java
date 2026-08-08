package com.logistics.userservice.application.dto;

import java.util.UUID;

public record AdminRejectCommand(UUID userId, String reason) {}
