package com.logistics.deliveryservice.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.logistics.deliveryservice.domain.exception.DeliveryErrorCode;
import com.logistics.deliveryservice.domain.exception.DeliveryException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeliveryCreationTest {

    private static final UUID ORDER_ID = UUID.fromString("09cf5d6c-ec32-43f9-871c-e5f152aa17e0");
    private static final UUID REQUESTER_ID = UUID.fromString("31a6c848-606d-4ca8-a8f9-006d348445cc");
    private static final UUID DEPARTURE_HUB_ID = UUID.fromString("0804066f-aadd-4b05-b7dc-7b0132040c4c");
    private static final UUID MIDDLE_HUB_ID = UUID.fromString("6b9cbad9-a9a6-453f-a124-d6de035fd214");
    private static final UUID ARRIVAL_HUB_ID = UUID.fromString("5aee128f-3f59-43f5-95b3-bd3638c4d968");
    private static final UUID COMPANY_MANAGER_ID = UUID.fromString("632c0753-9e77-4d76-92fa-fc201617a055");
    private static final UUID HUB_MANAGER_ID_1 = UUID.fromString("1194b39d-572d-4883-8577-859ddf06a5a1");
    private static final UUID HUB_MANAGER_ID_2 = UUID.fromString("8ec02ebd-7d84-4822-9172-b356947ee1e6");

    @Test
    void createsHubWaitingDeliveryAndWaitingRoutesFromValidPlan() {
        Delivery delivery = createDelivery(validPlan());

        assertThat(delivery.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.HUB_WAIT);
        assertThat(delivery.getDeliveryManagerId()).isEqualTo(COMPANY_MANAGER_ID);
        assertThat(delivery.getRouteHistories())
                .extracting(
                        DeliveryRouteHistory::getSequence,
                        DeliveryRouteHistory::getDepartureHubId,
                        DeliveryRouteHistory::getArrivalHubId,
                        DeliveryRouteHistory::getStatus
                )
                .containsExactly(
                        tuple(1, DEPARTURE_HUB_ID, MIDDLE_HUB_ID, RouteStatus.WAITING),
                        tuple(2, MIDDLE_HUB_ID, ARRIVAL_HUB_ID, RouteStatus.WAITING)
                );
        assertThat(delivery.getRouteHistories())
                .allSatisfy(route -> assertThat(route.getDelivery()).isSameAs(delivery));
    }

    @Test
    void createsSameHubDeliveryWithoutRoutesAsDestinationHubArrived() {
        DeliveryPlan plan = new DeliveryPlan(COMPANY_MANAGER_ID, List.of());

        Delivery delivery = Delivery.create(
                ORDER_ID,
                REQUESTER_ID,
                DEPARTURE_HUB_ID,
                DEPARTURE_HUB_ID,
                "서울시 중구 세종대로 1",
                "홍길동",
                null,
                plan
        );

        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.HUB_ARRIVED);
        assertThat(delivery.getRouteHistories()).isEmpty();
    }

    @Test
    void comparesOnlyOriginalImmutableRequestPayload() {
        Delivery delivery = createDelivery(validPlan());

        assertThat(delivery.hasSameImmutablePayload(
                REQUESTER_ID,
                DEPARTURE_HUB_ID,
                ARRIVAL_HUB_ID,
                "서울시 중구 세종대로 1",
                "홍길동",
                "receiver"
        )).isTrue();
        assertThat(delivery.hasSameImmutablePayload(
                REQUESTER_ID,
                DEPARTURE_HUB_ID,
                ARRIVAL_HUB_ID,
                "다른 주소",
                "홍길동",
                "receiver"
        )).isFalse();
        assertThat(delivery.isRecreationBlocked()).isFalse();
    }

    @Test
    void rejectsMissingOrNonContinuousRoutePlan() {
        assertInvalidPlan(new DeliveryPlan(COMPANY_MANAGER_ID, List.of()));
        assertInvalidPlan(new DeliveryPlan(COMPANY_MANAGER_ID, List.of(
                route(2, DEPARTURE_HUB_ID, ARRIVAL_HUB_ID, BigDecimal.ONE, 1, HUB_MANAGER_ID_1)
        )));
    }

    @Test
    void rejectsDisconnectedOrMismatchedHubPlan() {
        assertInvalidPlan(new DeliveryPlan(COMPANY_MANAGER_ID, List.of(
                route(1, MIDDLE_HUB_ID, ARRIVAL_HUB_ID, BigDecimal.ONE, 1, HUB_MANAGER_ID_1)
        )));
        assertInvalidPlan(new DeliveryPlan(COMPANY_MANAGER_ID, List.of(
                route(1, DEPARTURE_HUB_ID, MIDDLE_HUB_ID, BigDecimal.ONE, 1, HUB_MANAGER_ID_1),
                route(2, DEPARTURE_HUB_ID, ARRIVAL_HUB_ID, BigDecimal.ONE, 1, HUB_MANAGER_ID_2)
        )));
        assertInvalidPlan(new DeliveryPlan(COMPANY_MANAGER_ID, List.of(
                route(1, DEPARTURE_HUB_ID, MIDDLE_HUB_ID, BigDecimal.ONE, 1, HUB_MANAGER_ID_1)
        )));
    }

    @Test
    void rejectsMissingManagersAndNegativeEstimate() {
        assertInvalidPlan(new DeliveryPlan(null, validPlan().routes()));
        assertInvalidPlan(new DeliveryPlan(COMPANY_MANAGER_ID, List.of(
                route(1, DEPARTURE_HUB_ID, ARRIVAL_HUB_ID, BigDecimal.ONE, 1, null)
        )));
        assertInvalidPlan(new DeliveryPlan(COMPANY_MANAGER_ID, List.of(
                route(1, DEPARTURE_HUB_ID, ARRIVAL_HUB_ID, new BigDecimal("-0.1"), 1, HUB_MANAGER_ID_1)
        )));
        assertInvalidPlan(new DeliveryPlan(COMPANY_MANAGER_ID, List.of(
                route(1, DEPARTURE_HUB_ID, ARRIVAL_HUB_ID, BigDecimal.ONE, -1, HUB_MANAGER_ID_1)
        )));
    }

    @Test
    void rejectsRoutesForSameHubDelivery() {
        DeliveryPlan plan = new DeliveryPlan(COMPANY_MANAGER_ID, List.of(
                route(1, DEPARTURE_HUB_ID, DEPARTURE_HUB_ID, BigDecimal.ZERO, 0, HUB_MANAGER_ID_1)
        ));

        assertThatThrownBy(() -> Delivery.create(
                ORDER_ID,
                REQUESTER_ID,
                DEPARTURE_HUB_ID,
                DEPARTURE_HUB_ID,
                "서울시 중구 세종대로 1",
                "홍길동",
                "receiver",
                plan
        )).isInstanceOfSatisfying(DeliveryException.class, exception ->
                assertThat(exception.getErrorCode())
                        .isSameAs(DeliveryErrorCode.INVALID_HUB_DELIVERY_PLAN));
    }

    private void assertInvalidPlan(DeliveryPlan plan) {
        assertThatThrownBy(() -> createDelivery(plan))
                .isInstanceOfSatisfying(DeliveryException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isSameAs(DeliveryErrorCode.INVALID_HUB_DELIVERY_PLAN));
    }

    private Delivery createDelivery(DeliveryPlan plan) {
        return Delivery.create(
                ORDER_ID,
                REQUESTER_ID,
                DEPARTURE_HUB_ID,
                ARRIVAL_HUB_ID,
                "서울시 중구 세종대로 1",
                "홍길동",
                "receiver",
                plan
        );
    }

    private DeliveryPlan validPlan() {
        return new DeliveryPlan(COMPANY_MANAGER_ID, List.of(
                route(1, DEPARTURE_HUB_ID, MIDDLE_HUB_ID, new BigDecimal("10.5"), 25, HUB_MANAGER_ID_1),
                route(2, MIDDLE_HUB_ID, ARRIVAL_HUB_ID, new BigDecimal("8.3"), 18, HUB_MANAGER_ID_2)
        ));
    }

    private DeliveryPlan.Route route(
            int sequence,
            UUID departureHubId,
            UUID arrivalHubId,
            BigDecimal distance,
            int duration,
            UUID hubDeliveryManagerId
    ) {
        return new DeliveryPlan.Route(
                sequence,
                departureHubId,
                arrivalHubId,
                distance,
                duration,
                hubDeliveryManagerId
        );
    }
}
