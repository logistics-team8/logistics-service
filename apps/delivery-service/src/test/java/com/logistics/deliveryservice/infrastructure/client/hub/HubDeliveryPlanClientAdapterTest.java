package com.logistics.deliveryservice.infrastructure.client.hub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.logistics.deliveryservice.domain.exception.DeliveryErrorCode;
import com.logistics.deliveryservice.domain.exception.DeliveryException;
import com.logistics.deliveryservice.domain.model.DeliveryPlan;
import com.logistics.deliveryservice.infrastructure.client.hub.dto.HubDeliveryPlanRequest;
import com.logistics.deliveryservice.infrastructure.client.hub.dto.HubDeliveryPlanResponse;
import feign.FeignException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HubDeliveryPlanClientAdapterTest {

    private static final UUID ORDER_ID = UUID.fromString("aacdd677-e9a5-4053-9c29-a455dfe48a92");
    private static final UUID DEPARTURE_HUB_ID = UUID.fromString("c5187423-653a-4203-a581-19e7124fe295");
    private static final UUID ARRIVAL_HUB_ID = UUID.fromString("68631ad3-f9f3-458b-8dd0-10d60257dc50");

    @Mock
    private HubDeliveryPlanFeignClient feignClient;

    @InjectMocks
    private HubDeliveryPlanClientAdapter adapter;

    @Test
    void mapsFeignResponseToDomainPlan() {
        HubDeliveryPlanRequest request = new HubDeliveryPlanRequest(
                ORDER_ID,
                DEPARTURE_HUB_ID,
                ARRIVAL_HUB_ID
        );
        HubDeliveryPlanResponse response = new HubDeliveryPlanResponse(
                List.of(new HubDeliveryPlanResponse.RouteResponse(
                        1,
                        DEPARTURE_HUB_ID,
                        ARRIVAL_HUB_ID,
                        new BigDecimal("14.5"),
                        35
                ))
        );
        when(feignClient.createDeliveryPlan(request)).thenReturn(response);

        DeliveryPlan plan = adapter.getDeliveryPlan(
                ORDER_ID,
                DEPARTURE_HUB_ID,
                ARRIVAL_HUB_ID
        );

        assertThat(plan.companyDeliveryManagerId()).isNull();
        assertThat(plan.routes()).singleElement().satisfies(route -> {
            assertThat(route.sequence()).isEqualTo(1);
            assertThat(route.departureHubId()).isEqualTo(DEPARTURE_HUB_ID);
            assertThat(route.arrivalHubId()).isEqualTo(ARRIVAL_HUB_ID);
            assertThat(route.estimatedDistanceKm()).isEqualByComparingTo("14.5");
            assertThat(route.estimatedDurationMinutes()).isEqualTo(35);
            assertThat(route.hubDeliveryManagerId()).isNull();
        });
        verify(feignClient).createDeliveryPlan(request);
    }

    @Test
    void convertsNullResponseToInvalidPlanError() {
        when(feignClient.createDeliveryPlan(new HubDeliveryPlanRequest(
                ORDER_ID,
                DEPARTURE_HUB_ID,
                ARRIVAL_HUB_ID
        ))).thenReturn(null);

        assertThatThrownBy(() -> adapter.getDeliveryPlan(
                ORDER_ID,
                DEPARTURE_HUB_ID,
                ARRIVAL_HUB_ID
        )).isInstanceOfSatisfying(DeliveryException.class, exception ->
                assertThat(exception.getErrorCode())
                        .isSameAs(DeliveryErrorCode.INVALID_HUB_DELIVERY_PLAN));
    }

    @Test
    void convertsFeignFailureToUnavailableError() {
        HubDeliveryPlanRequest request = new HubDeliveryPlanRequest(
                ORDER_ID,
                DEPARTURE_HUB_ID,
                ARRIVAL_HUB_ID
        );
        FeignException cause = mock(FeignException.class);
        when(feignClient.createDeliveryPlan(request)).thenThrow(cause);

        assertThatThrownBy(() -> adapter.getDeliveryPlan(
                ORDER_ID,
                DEPARTURE_HUB_ID,
                ARRIVAL_HUB_ID
        )).isInstanceOfSatisfying(DeliveryException.class, exception -> {
            assertThat(exception.getErrorCode())
                    .isSameAs(DeliveryErrorCode.HUB_DELIVERY_PLAN_UNAVAILABLE);
            assertThat(exception.getCause()).isSameAs(cause);
        });
    }
}
