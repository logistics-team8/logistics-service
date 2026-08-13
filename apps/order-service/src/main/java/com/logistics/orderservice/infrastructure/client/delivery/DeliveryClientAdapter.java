package com.logistics.orderservice.infrastructure.client.delivery;

import com.logistics.orderservice.application.exception.DeliveryCreateException;
import com.logistics.orderservice.application.exception.DeliveryCreateRejectedException;
import com.logistics.orderservice.application.exception.DeliveryCreateRetryableException;
import com.logistics.orderservice.application.exception.DeliveryLookupException;
import com.logistics.orderservice.application.port.DeliveryPort;
import feign.FeignException;
import feign.RetryableException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

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

        try {
            DeliveryFeignClient.CreateDeliveryResponse response = deliveryFeignClient
                            .createDelivery(request)
                            .getData();

            if (response == null) {
                throw new DeliveryCreateRetryableException(
                        "배송 생성 응답 데이터가 없어 처리 결과를 확인할 수 없습니다."
                );
            }
            return toDeliveryInfo(response);

        } catch (RetryableException e) {
            throw new DeliveryCreateRetryableException(
                    "배송 생성 요청의 처리 결과를 확인할 수 없습니다.", e
            );
        } catch (FeignException e) {
            if (isTransientStatus(e.status())) {
                throw new DeliveryCreateRetryableException(
                        "배송 서비스의 일시적 장애로 생성 요청에 실패했습니다.", e
                );
            }
            throw new DeliveryCreateRejectedException(
                    "재시도할 수 없는 배송 생성 요청 실패입니다.", e
            );
        }

    }

    private boolean isTransientStatus(int status) {
        return status == 502 || status == 503 || status == 504;
    }

    @Override
    public Optional<DeliveryInfo> findDeliveryByOrderId(UUID orderId) {
        try {
            DeliveryFeignClient.GetDeliveryByOrderResponse response = deliveryFeignClient
                            .getDeliveryByOrder(orderId)
                            .getData();

            if (response == null) {
                throw new DeliveryLookupException("배송 조회 응답 데이터가 없습니다.");
            }

            return Optional.of(toDeliveryInfo(response));

        } catch (FeignException.NotFound e) {

            return Optional.empty();

        } catch (FeignException e) {
            throw new DeliveryLookupException("배송 조회 요청에 실패했습니다.", e);
        }
    }

    private DeliveryInfo toDeliveryInfo(
            DeliveryFeignClient.CreateDeliveryResponse response
    ) {
        return new DeliveryInfo(
                response.deliveryId(),
                response.orderId(),
                response.requesterId(),
                response.status(),
                response.departureHubId(),
                response.arrivalHubId(),
                response.deliveryAddress(),
                response.receiverName(),
                response.receiverSlackId()
        );
    }


    private DeliveryInfo toDeliveryInfo(
            DeliveryFeignClient.GetDeliveryByOrderResponse response
    ) {
        return new DeliveryInfo(
                response.deliveryId(),
                response.orderId(),
                response.requesterId(),
                response.status(),
                response.departureHubId(),
                response.arrivalHubId(),
                response.deliveryAddress(),
                response.receiverName(),
                response.receiverSlackId()
        );
    }

}
