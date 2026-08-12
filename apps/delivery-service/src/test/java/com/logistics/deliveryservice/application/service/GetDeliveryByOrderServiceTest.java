package com.logistics.deliveryservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.logistics.deliveryservice.application.dto.GetDeliveryByOrderResponse;
import com.logistics.deliveryservice.domain.exception.DeliveryErrorCode;
import com.logistics.deliveryservice.domain.exception.DeliveryException;
import com.logistics.deliveryservice.domain.model.Delivery;
import com.logistics.deliveryservice.domain.model.DeliveryRouteHistory;
import com.logistics.deliveryservice.domain.model.DeliveryStatus;
import com.logistics.deliveryservice.domain.repository.DeliveryRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetDeliveryByOrderServiceTest {

    private static final UUID DELIVERY_ID = UUID.fromString("8ec61e37-bab0-43ae-9185-0454f012940a");
    private static final UUID ORDER_ID = UUID.fromString("9ec77589-61a1-4f18-957e-6d92f934bd2d");

    @Mock
    private DeliveryRepository deliveryRepository;

    @InjectMocks
    private GetDeliveryByOrderService getDeliveryByOrderService;

    @Test
    void returnsActiveDeliveryWithRoutesSortedBySequence() {
        Delivery delivery = mock(Delivery.class);
        DeliveryRouteHistory firstRoute = mock(DeliveryRouteHistory.class);
        DeliveryRouteHistory secondRoute = mock(DeliveryRouteHistory.class);
        when(delivery.getDeliveryId()).thenReturn(DELIVERY_ID);
        when(delivery.getOrderId()).thenReturn(ORDER_ID);
        when(delivery.getStatus()).thenReturn(DeliveryStatus.HUB_WAITING);
        when(delivery.getRouteHistories()).thenReturn(List.of(secondRoute, firstRoute));
        when(firstRoute.getSequence()).thenReturn(1);
        when(secondRoute.getSequence()).thenReturn(2);
        when(deliveryRepository.findActiveByOrderId(ORDER_ID)).thenReturn(Optional.of(delivery));

        GetDeliveryByOrderResponse response = getDeliveryByOrderService.getByOrderId(ORDER_ID);

        assertThat(response.deliveryId()).isEqualTo(DELIVERY_ID);
        assertThat(response.orderId()).isEqualTo(ORDER_ID);
        assertThat(response.status()).isEqualTo(DeliveryStatus.HUB_WAITING);
        assertThat(response.routes())
                .extracting(GetDeliveryByOrderResponse.RouteResponse::sequence)
                .containsExactly(1, 2);
        verify(deliveryRepository).findActiveByOrderId(ORDER_ID);
    }

    @Test
    void throwsNotFoundWhenActiveDeliveryDoesNotExist() {
        when(deliveryRepository.findActiveByOrderId(ORDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getDeliveryByOrderService.getByOrderId(ORDER_ID))
                .isInstanceOfSatisfying(DeliveryException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isSameAs(DeliveryErrorCode.DELIVERY_NOT_FOUND));
        verify(deliveryRepository).findActiveByOrderId(ORDER_ID);
    }
}
