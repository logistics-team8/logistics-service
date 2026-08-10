package com.logistics.userservice.application.event;

import java.util.UUID;

public record UserDeletedEvent(UUID userId) {}
