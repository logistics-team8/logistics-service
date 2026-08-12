package com.logistics.deliveryservice.application.service;

import com.logistics.deliveryservice.application.dto.GetDeliveryByOrderResponse;
import com.logistics.deliveryservice.domain.exception.DeliveryErrorCode;
import com.logistics.deliveryservice.domain.exception.DeliveryException;
import com.logistics.deliveryservice.domain.model.Delivery;
import com.logistics.deliveryservice.domain.repository.DeliveryRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Order Service가 주문 ID로 활성 배송을 확인하는 조회 유스케이스를 처리한다.
 */
@Service
@RequiredArgsConstructor
public class GetDeliveryByOrderService {

    private final DeliveryRepository deliveryRepository;

    /**
     * 논리 삭제되지 않은 배송만 조회하고 Order Service용 응답으로 변환한다.
     */
    public GetDeliveryByOrderResponse getByOrderId(UUID orderId) {
        // 취소·삭제된 배송을 내부 조회 결과로 노출하지 않도록 활성 데이터 전용 조회를 사용한다.
        Delivery delivery = deliveryRepository.findActiveByOrderId(orderId)
                .orElseThrow(() -> new DeliveryException(DeliveryErrorCode.DELIVERY_NOT_FOUND));

        return GetDeliveryByOrderResponse.from(delivery);
    }
}
