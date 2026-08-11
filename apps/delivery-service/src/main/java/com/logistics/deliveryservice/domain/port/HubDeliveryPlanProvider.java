package com.logistics.deliveryservice.domain.port;

import com.logistics.deliveryservice.domain.model.DeliveryPlan;
import java.util.UUID;

/**
 * 주문의 출발·도착 허브를 기준으로 Hub 배송 계획을 가져오는 외부 연동 포트다.
 */
public interface HubDeliveryPlanProvider {

    DeliveryPlan getDeliveryPlan(UUID orderId, UUID departureHubId, UUID arrivalHubId);
}
