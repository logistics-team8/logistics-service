package com.logistics.deliveryservice.domain.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Hub Service가 계산한 업체 담당자와 허브 간 전체 배송 경로 계획이다.
 */
public record DeliveryPlan(
        UUID companyDeliveryManagerId,
        List<Route> routes
) {

    public DeliveryPlan {
        routes = routes == null
                ? null
                : Collections.unmodifiableList(new ArrayList<>(routes));
    }

    /**
     * 전체 배송 계획에 포함된 하나의 허브 간 이동 구간이다.
     */
    public record Route(
            Integer sequence,
            UUID departureHubId,
            UUID arrivalHubId,
            BigDecimal estimatedDistanceKm,
            Integer estimatedDurationMinutes,
            UUID hubDeliveryManagerId
    ) {
    }
}
