package com.logistics.deliveryservice.presentation.dto;

import com.logistics.deliveryservice.application.command.CreateDeliveryManagerCommand;
import com.logistics.deliveryservice.domain.model.DeliveryManagerType;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;


public record CreateDeliveryManagerRequest(

        @NotNull(message = "배송 담당자 사용자 ID는 필수입니다.")
        UUID userId,

        @NotNull(message = "배송 담당자 유형은 필수입니다.")
        DeliveryManagerType managerType,

        @NotNull(message = "담당 허브 ID는 필수입니다.")
        UUID hubId
) {

    public CreateDeliveryManagerCommand toCommand() {
        return new CreateDeliveryManagerCommand(userId, managerType, hubId);
    }
}
