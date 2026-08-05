package com.logistics.hubservice.presentation.hub.dto;

import com.logistics.hubservice.application.hub.query.HubResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record HubResponseDto(
        UUID hubId,
        String name,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static HubResponseDto from(HubResponse response) {
        return new HubResponseDto(
                response.hubId(),
                response.name(),
                response.address(),
                response.latitude(),
                response.longitude(),
                response.createdAt(),
                response.updatedAt()
        );
    }
}
