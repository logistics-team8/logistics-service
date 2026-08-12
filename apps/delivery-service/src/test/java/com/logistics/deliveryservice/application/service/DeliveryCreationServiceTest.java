package com.logistics.deliveryservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.logistics.deliveryservice.application.command.DeliveryCreateCommand;
import com.logistics.deliveryservice.domain.model.Delivery;
import com.logistics.deliveryservice.domain.model.DeliveryPlan;
import com.logistics.deliveryservice.domain.repository.DeliveryRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeliveryCreationServiceTest {

    private static final UUID ORDER_ID = UUID.fromString("571ea3ed-ec73-4158-aaed-f157b76dd7f7");
    private static final UUID REQUESTER_ID = UUID.fromString("86d6d4a2-b267-4ae6-8520-9fca2f990079");
    private static final UUID DEPARTURE_HUB_ID = UUID.fromString("1e72b7f7-4c59-49ab-837c-e2c5e9a7ad79");
    private static final UUID ARRIVAL_HUB_ID = UUID.fromString("58956a26-b725-4d12-bba1-f882032284be");
    private static final UUID COMPANY_MANAGER_ID = UUID.fromString("f0a777f4-2f58-48c9-8228-22b43a497185");
    private static final UUID HUB_MANAGER_ID = UUID.fromString("d99fd717-68f9-4275-b4c6-7014271f8b1f");

    @Mock
    private DeliveryManagerAssignmentService assignmentService;

    @Mock
    private DeliveryRepository deliveryRepository;

    @InjectMocks
    private DeliveryCreationService deliveryCreationService;

    @Test
    void assignsManagersThenSavesCreatedDelivery() {
        DeliveryCreateCommand command = command();
        DeliveryPlan hubPlan = unassignedPlan();
        DeliveryPlan assignedPlan = assignedPlan();
        when(assignmentService.assignManagers(hubPlan, ARRIVAL_HUB_ID)).thenReturn(assignedPlan);
        when(deliveryRepository.save(any(Delivery.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Delivery saved = deliveryCreationService.register(command, hubPlan);

        assertThat(saved.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(saved.getCompanyDeliveryManagerId()).isEqualTo(COMPANY_MANAGER_ID);
        assertThat(saved.getRouteHistories()).hasSize(1);
        assertThat(saved.getRouteHistories().get(0).getHubDeliveryManagerId())
                .isEqualTo(HUB_MANAGER_ID);
        verify(assignmentService).assignManagers(hubPlan, ARRIVAL_HUB_ID);
        verify(deliveryRepository).save(saved);
    }

    private DeliveryCreateCommand command() {
        return new DeliveryCreateCommand(
                ORDER_ID,
                REQUESTER_ID,
                DEPARTURE_HUB_ID,
                ARRIVAL_HUB_ID,
                "서울시 중구 세종대로 1",
                "홍길동",
                "receiver"
        );
    }

    private DeliveryPlan unassignedPlan() {
        return new DeliveryPlan(null, List.of(route(null)));
    }

    private DeliveryPlan assignedPlan() {
        return new DeliveryPlan(COMPANY_MANAGER_ID, List.of(route(HUB_MANAGER_ID)));
    }

    private DeliveryPlan.Route route(UUID hubDeliveryManagerId) {
        return new DeliveryPlan.Route(
                1,
                DEPARTURE_HUB_ID,
                ARRIVAL_HUB_ID,
                new BigDecimal("12.4"),
                30,
                hubDeliveryManagerId
        );
    }
}
