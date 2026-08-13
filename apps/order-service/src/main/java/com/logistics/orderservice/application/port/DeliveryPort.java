package com.logistics.orderservice.application.port;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryPort {
    DeliveryInfo createDelivery(CreateDeliveryCommand deliveryCommand);

    Optional<DeliveryInfo> findDeliveryByOrderId(UUID orderId);

    record CreateDeliveryCommand(
            UUID orderId,
            UUID requesterId,
            UUID departureHubId,
            UUID arrivalHubId,
            String deliveryAddress,
            String receiverName,
            String receiverSlackId
    ){
    }

    record DeliveryInfo(
            UUID deliveryId,
            UUID orderId,
            UUID requesterId,
            String status,
            UUID departureHubId,
            UUID arrivalHubId,
            String deliveryAddress,
            String receiverName,
            String receiverSlackId
    ) {

        public boolean matches(
                CreateDeliveryCommand command
        ) {
            return Objects.equals(
                    orderId,
                    command.orderId()
            )
                    && Objects.equals(
                    requesterId,
                    command.requesterId()
            )
                    && Objects.equals(
                    departureHubId,
                    command.departureHubId()
            )
                    && Objects.equals(
                    arrivalHubId,
                    command.arrivalHubId()
            )
                    && Objects.equals(
                    deliveryAddress,
                    command.deliveryAddress()
            )
                    && Objects.equals(
                    receiverName,
                    command.receiverName()
            )
                    && Objects.equals(
                    receiverSlackId,
                    command.receiverSlackId()
            );
        }
    }


}
