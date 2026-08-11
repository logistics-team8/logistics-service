package com.logistics.deliveryservice.infrastructure.client.hub;

import com.logistics.deliveryservice.domain.exception.DeliveryErrorCode;
import com.logistics.deliveryservice.domain.exception.DeliveryException;
import com.logistics.deliveryservice.domain.model.DeliveryPlan;
import com.logistics.deliveryservice.domain.port.HubDeliveryPlanProvider;
import com.logistics.deliveryservice.infrastructure.client.hub.dto.HubDeliveryPlanRequest;
import com.logistics.deliveryservice.infrastructure.client.hub.dto.HubDeliveryPlanResponse;
import feign.FeignException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Feign 계약 DTO를 Delivery 도메인 계획으로 변환하고 호출 실패를 서비스 오류로 바꾼다.
 */
@Component
@RequiredArgsConstructor
public class HubDeliveryPlanClientAdapter implements HubDeliveryPlanProvider {

    private final HubDeliveryPlanFeignClient hubDeliveryPlanFeignClient;

    @Override
    public DeliveryPlan getDeliveryPlan(
            UUID orderId,
            UUID departureHubId,
            UUID arrivalHubId
    ) {
        // Application 계층의 입력값을 Hub Service가 요구하는 외부 API 계약으로 변환한다.
        HubDeliveryPlanRequest request = new HubDeliveryPlanRequest(
                orderId,
                departureHubId,
                arrivalHubId
        );

        try {
            // Infrastructure DTO가 도메인으로 새지 않도록 응답을 DeliveryPlan으로 변환한다.
            return toDeliveryPlan(hubDeliveryPlanFeignClient.createDeliveryPlan(request));
        } catch (FeignException exception) {
            // 네트워크·상태 코드 등 Feign 호출 실패를 Delivery의 통일된 외부 연동 오류로 바꾼다.
            throw new DeliveryException(
                    DeliveryErrorCode.HUB_DELIVERY_PLAN_UNAVAILABLE,
                    exception
            );
        }
    }

    private DeliveryPlan toDeliveryPlan(HubDeliveryPlanResponse response) {
        // 정상 호출이어도 응답 본문이 없으면 사용할 수 없는 Hub 계획으로 처리한다.
        if (response == null) {
            throw new DeliveryException(DeliveryErrorCode.INVALID_HUB_DELIVERY_PLAN);
        }

        List<DeliveryPlan.Route> routes = response.routes() == null
                ? null
                : response.routes().stream()
                .map(this::toRoutePlan)
                .toList();
        return new DeliveryPlan(response.companyDeliveryManagerId(), routes);
    }

    private DeliveryPlan.Route toRoutePlan(HubDeliveryPlanResponse.RouteResponse route) {
        // 잘못된 구간을 임의로 제거하지 않고 null을 유지하여 Domain 검증에서 전체 계획을 거부한다.
        if (route == null) {
            return null;
        }
        return new DeliveryPlan.Route(
                route.sequence(),
                route.departureHubId(),
                route.arrivalHubId(),
                route.estimatedDistanceKm(),
                route.estimatedDurationMinutes(),
                route.hubDeliveryManagerId()
        );
    }
}
