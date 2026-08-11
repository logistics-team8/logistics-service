package com.logistics.orderservice.infrastructure.client.delivery;

import com.logistics.orderservice.application.port.DeliveryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeliveryClientAdapter implements DeliveryPort {

    private final DeliveryFeignClient deliveryFeignClient;


    @Override
    public DeliveryInfo createDelivery(CreateDeliveryCommand command) {
        DeliveryFeignClient.CreateDeliveryRequest request =
                new DeliveryFeignClient.CreateDeliveryRequest(
                        command.orderId(),
                        command.requesterId(),
                        command.departureHubId(),
                        command.arrivalHubId(),
                        command.deliveryAddress(),
                        command.receiverName(),
                        command.receiverSlackId()
                );

        DeliveryFeignClient.CreateDeliveryResponse response =
                deliveryFeignClient.createDelivery(request).getData();


        if(response == null){
            throw new IllegalArgumentException("배송 생성 응답 데이터가 없습니다.");
        }

        return new DeliveryInfo(
                response.deliveryId(),
                response.orderId(),
                response.status()
        );
    }
}
