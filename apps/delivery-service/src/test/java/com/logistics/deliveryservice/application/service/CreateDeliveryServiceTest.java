package com.logistics.deliveryservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.logistics.deliveryservice.application.command.CreateDeliveryCommand;
import com.logistics.deliveryservice.application.dto.CreateDeliveryResult;
import com.logistics.deliveryservice.domain.exception.DeliveryErrorCode;
import com.logistics.deliveryservice.domain.exception.DeliveryException;
import com.logistics.deliveryservice.domain.model.Delivery;
import com.logistics.deliveryservice.domain.model.DeliveryPlan;
import com.logistics.deliveryservice.domain.port.HubDeliveryPlanProvider;
import com.logistics.deliveryservice.domain.repository.DeliveryRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class CreateDeliveryServiceTest {

    private static final UUID ORDER_ID = UUID.fromString("571ea3ed-ec73-4158-aaed-f157b76dd7f7");
    private static final UUID REQUESTER_ID = UUID.fromString("86d6d4a2-b267-4ae6-8520-9fca2f990079");
    private static final UUID DEPARTURE_HUB_ID = UUID.fromString("1e72b7f7-4c59-49ab-837c-e2c5e9a7ad79");
    private static final UUID ARRIVAL_HUB_ID = UUID.fromString("58956a26-b725-4d12-bba1-f882032284be");
    private static final UUID COMPANY_MANAGER_ID = UUID.fromString("f0a777f4-2f58-48c9-8228-22b43a497185");
    private static final UUID HUB_MANAGER_ID = UUID.fromString("d99fd717-68f9-4275-b4c6-7014271f8b1f");

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private HubDeliveryPlanProvider hubDeliveryPlanProvider;

    @InjectMocks
    private CreateDeliveryService createDeliveryService;

    @Test
    void createsDeliveryAfterFetchingHubPlanOnce() {
        CreateDeliveryCommand command = command("서울시 중구 세종대로 1");
        DeliveryPlan plan = plan();
        when(deliveryRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());
        when(hubDeliveryPlanProvider.getDeliveryPlan(
                ORDER_ID,
                DEPARTURE_HUB_ID,
                ARRIVAL_HUB_ID
        )).thenReturn(plan);
        when(deliveryRepository.save(any(Delivery.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreateDeliveryResult result = createDeliveryService.create(command);

        assertThat(result.created()).isTrue();
        assertThat(result.response().orderId()).isEqualTo(ORDER_ID);
        assertThat(result.response().routes()).hasSize(1);
        verify(hubDeliveryPlanProvider).getDeliveryPlan(
                ORDER_ID,
                DEPARTURE_HUB_ID,
                ARRIVAL_HUB_ID
        );
        verify(deliveryRepository).save(any(Delivery.class));
    }

    @Test
    void returnsExistingDeliveryWithoutCallingHubForSamePayload() {
        CreateDeliveryCommand command = command("서울시 중구 세종대로 1");
        Delivery existingDelivery = delivery(command, plan());
        when(deliveryRepository.findByOrderId(ORDER_ID))
                .thenReturn(Optional.of(existingDelivery));

        CreateDeliveryResult result = createDeliveryService.create(command);

        assertThat(result.created()).isFalse();
        assertThat(result.response().orderId()).isEqualTo(ORDER_ID);
        verifyNoInteractions(hubDeliveryPlanProvider);
        verify(deliveryRepository, never()).save(any());
    }

    @Test
    void rejectsDifferentPayloadForExistingOrder() {
        CreateDeliveryCommand command = command("변경된 배송 주소");
        Delivery existingDelivery = delivery(
                command("서울시 중구 세종대로 1"),
                plan()
        );
        when(deliveryRepository.findByOrderId(ORDER_ID))
                .thenReturn(Optional.of(existingDelivery));

        assertDuplicateConflict(() -> createDeliveryService.create(command));
        verifyNoInteractions(hubDeliveryPlanProvider);
    }

    @Test
    void rejectsCanceledOrDeletedExistingDelivery() {
        CreateDeliveryCommand command = command("서울시 중구 세종대로 1");
        Delivery existingDelivery = mock(Delivery.class);
        when(existingDelivery.isRecreationBlocked()).thenReturn(true);
        when(deliveryRepository.findByOrderId(ORDER_ID))
                .thenReturn(Optional.of(existingDelivery));

        assertDuplicateConflict(() -> createDeliveryService.create(command));
        verifyNoInteractions(hubDeliveryPlanProvider);
    }

    @Test
    void resolvesUniqueRaceAsIdempotentWhenConcurrentPayloadIsSame() {
        CreateDeliveryCommand command = command("서울시 중구 세종대로 1");
        Delivery concurrentDelivery = delivery(command, plan());
        when(deliveryRepository.findByOrderId(ORDER_ID))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(concurrentDelivery));
        when(hubDeliveryPlanProvider.getDeliveryPlan(
                ORDER_ID,
                DEPARTURE_HUB_ID,
                ARRIVAL_HUB_ID
        )).thenReturn(plan());
        when(deliveryRepository.save(any(Delivery.class)))
                .thenThrow(new DataIntegrityViolationException("order_id unique"));

        CreateDeliveryResult result = createDeliveryService.create(command);

        assertThat(result.created()).isFalse();
        assertThat(result.response().orderId()).isEqualTo(ORDER_ID);
    }

    @Test
    void resolvesUniqueRaceAsConflictWhenConcurrentPayloadDiffers() {
        CreateDeliveryCommand command = command("서울시 중구 세종대로 1");
        Delivery concurrentDelivery = delivery(command("다른 주소"), plan());
        when(deliveryRepository.findByOrderId(ORDER_ID))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(concurrentDelivery));
        when(hubDeliveryPlanProvider.getDeliveryPlan(
                ORDER_ID,
                DEPARTURE_HUB_ID,
                ARRIVAL_HUB_ID
        )).thenReturn(plan());
        when(deliveryRepository.save(any(Delivery.class)))
                .thenThrow(new DataIntegrityViolationException("order_id unique"));

        assertDuplicateConflict(() -> createDeliveryService.create(command));
    }

    @Test
    void rethrowsUnexpectedIntegrityViolationWhenNoConcurrentOrderExists() {
        CreateDeliveryCommand command = command("서울시 중구 세종대로 1");
        DataIntegrityViolationException violation =
                new DataIntegrityViolationException("unrelated constraint");
        when(deliveryRepository.findByOrderId(ORDER_ID))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.empty());
        when(hubDeliveryPlanProvider.getDeliveryPlan(
                ORDER_ID,
                DEPARTURE_HUB_ID,
                ARRIVAL_HUB_ID
        )).thenReturn(plan());
        when(deliveryRepository.save(any(Delivery.class))).thenThrow(violation);

        assertThatThrownBy(() -> createDeliveryService.create(command)).isSameAs(violation);
    }

    private void assertDuplicateConflict(Runnable invocation) {
        assertThatThrownBy(invocation::run)
                .isInstanceOfSatisfying(DeliveryException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isSameAs(DeliveryErrorCode.DUPLICATE_ORDER_DELIVERY));
    }

    private CreateDeliveryCommand command(String deliveryAddress) {
        return new CreateDeliveryCommand(
                ORDER_ID,
                REQUESTER_ID,
                DEPARTURE_HUB_ID,
                ARRIVAL_HUB_ID,
                deliveryAddress,
                "홍길동",
                "receiver"
        );
    }

    private DeliveryPlan plan() {
        return new DeliveryPlan(COMPANY_MANAGER_ID, List.of(
                new DeliveryPlan.Route(
                        1,
                        DEPARTURE_HUB_ID,
                        ARRIVAL_HUB_ID,
                        new BigDecimal("12.4"),
                        30,
                        HUB_MANAGER_ID
                )
        ));
    }

    private Delivery delivery(CreateDeliveryCommand command, DeliveryPlan plan) {
        return Delivery.create(
                command.orderId(),
                command.requesterId(),
                command.departureHubId(),
                command.arrivalHubId(),
                command.deliveryAddress(),
                command.receiverName(),
                command.receiverSlackId(),
                plan
        );
    }
}
