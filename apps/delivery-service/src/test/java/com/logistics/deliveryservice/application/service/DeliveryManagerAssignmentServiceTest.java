package com.logistics.deliveryservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.logistics.deliveryservice.domain.exception.DeliveryErrorCode;
import com.logistics.deliveryservice.domain.exception.DeliveryException;
import com.logistics.deliveryservice.domain.model.DeliveryManager;
import com.logistics.deliveryservice.domain.model.DeliveryManagerAssignmentCursor;
import com.logistics.deliveryservice.domain.model.DeliveryManagerAssignmentGroup;
import com.logistics.deliveryservice.domain.model.DeliveryPlan;
import com.logistics.deliveryservice.domain.repository.DeliveryManagerAssignmentCursorRepository;
import com.logistics.deliveryservice.domain.repository.DeliveryManagerRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeliveryManagerAssignmentServiceTest {

    private static final UUID DEPARTURE_HUB_ID =
            UUID.fromString("1e72b7f7-4c59-49ab-837c-e2c5e9a7ad79");
    private static final UUID ARRIVAL_HUB_ID =
            UUID.fromString("58956a26-b725-4d12-bba1-f882032284be");
    private static final UUID NEXT_HUB_ID =
            UUID.fromString("7c1a0d2e-3b44-4f8a-9c21-0a6b8d5e4f10");
    private static final UUID COMPANY_MANAGER_0 =
            UUID.fromString("f0a777f4-2f58-48c9-8228-22b43a497185");
    private static final UUID COMPANY_MANAGER_1 =
            UUID.fromString("a11b22c3-d44e-455f-8661-977288399001");
    private static final UUID HUB_MANAGER_0 =
            UUID.fromString("d99fd717-68f9-4275-b4c6-7014271f8b1f");
    private static final UUID HUB_MANAGER_1 =
            UUID.fromString("b88ec606-57e8-4164-a3b5-6103160e7a20");

    @Mock
    private DeliveryManagerAssignmentCursorRepository cursorRepository;

    @Mock
    private DeliveryManagerRepository deliveryManagerRepository;

    @InjectMocks
    private DeliveryManagerAssignmentService assignmentService;

    @Test
    void assignsCompanyAndHubManagersFromSmallestSequences() {
        DeliveryManagerAssignmentGroup companyGroup =
                DeliveryManagerAssignmentGroup.companyDelivery(ARRIVAL_HUB_ID);
        DeliveryManagerAssignmentGroup hubGroup =
                DeliveryManagerAssignmentGroup.hubDelivery(ARRIVAL_HUB_ID);
        stubCompanyManagers(companyGroup, List.of(
                companyManager(COMPANY_MANAGER_0, 0),
                companyManager(COMPANY_MANAGER_1, 1)
        ));
        stubHubManagers(hubGroup, List.of(
                hubManager(HUB_MANAGER_0, 0),
                hubManager(HUB_MANAGER_1, 1)
        ));

        DeliveryPlan assigned = assignmentService.assignManagers(unassignedPlan(), ARRIVAL_HUB_ID);

        assertThat(assigned.companyDeliveryManagerId()).isEqualTo(COMPANY_MANAGER_0);
        assertThat(assigned.routes()).hasSize(1);
        assertThat(assigned.routes().get(0).hubDeliveryManagerId()).isEqualTo(HUB_MANAGER_0);
        verify(cursorRepository).acquireForUpdate(companyGroup);
        verify(cursorRepository).acquireForUpdate(hubGroup);
    }

    @Test
    void advancesHubManagerPerRoute() {
        DeliveryManagerAssignmentGroup companyGroup =
                DeliveryManagerAssignmentGroup.companyDelivery(ARRIVAL_HUB_ID);
        DeliveryManagerAssignmentGroup hubGroup =
                DeliveryManagerAssignmentGroup.hubDelivery(ARRIVAL_HUB_ID);
        stubCompanyManagers(companyGroup, List.of(companyManager(COMPANY_MANAGER_0, 0)));
        stubHubManagers(hubGroup, List.of(
                hubManager(HUB_MANAGER_0, 0),
                hubManager(HUB_MANAGER_1, 1)
        ));

        DeliveryPlan assigned = assignmentService.assignManagers(
                unassignedTwoRoutePlan(),
                ARRIVAL_HUB_ID
        );

        assertThat(assigned.routes().get(0).hubDeliveryManagerId()).isEqualTo(HUB_MANAGER_0);
        assertThat(assigned.routes().get(1).hubDeliveryManagerId()).isEqualTo(HUB_MANAGER_1);
        verify(cursorRepository).acquireForUpdate(hubGroup);
    }

    @Test
    void skipsHubAssignmentWhenRoutesAreEmpty() {
        DeliveryManagerAssignmentGroup companyGroup =
                DeliveryManagerAssignmentGroup.companyDelivery(ARRIVAL_HUB_ID);
        DeliveryManagerAssignmentGroup hubGroup =
                DeliveryManagerAssignmentGroup.hubDelivery(ARRIVAL_HUB_ID);
        stubCompanyManagers(companyGroup, List.of(companyManager(COMPANY_MANAGER_0, 0)));

        DeliveryPlan assigned = assignmentService.assignManagers(
                new DeliveryPlan(null, List.of()),
                ARRIVAL_HUB_ID
        );

        assertThat(assigned.companyDeliveryManagerId()).isEqualTo(COMPANY_MANAGER_0);
        assertThat(assigned.routes()).isEmpty();
        verify(cursorRepository, never()).acquireForUpdate(hubGroup);
        verify(deliveryManagerRepository, never()).findActiveManagers(hubGroup);
    }

    @Test
    void rejectsEmptyCompanyManagerGroup() {
        DeliveryManagerAssignmentGroup companyGroup =
                DeliveryManagerAssignmentGroup.companyDelivery(ARRIVAL_HUB_ID);
        stubCompanyManagers(companyGroup, List.of());

        assertThatThrownBy(() -> assignmentService.assignManagers(
                unassignedPlan(),
                ARRIVAL_HUB_ID
        ))
                .isInstanceOf(DeliveryException.class)
                .extracting(exception -> ((DeliveryException) exception).getErrorCode())
                .isEqualTo(DeliveryErrorCode.DELIVERY_MANAGER_UNAVAILABLE);
    }

    @Test
    void rejectsEmptyHubManagerGroupWhenRoutesExist() {
        DeliveryManagerAssignmentGroup companyGroup =
                DeliveryManagerAssignmentGroup.companyDelivery(ARRIVAL_HUB_ID);
        DeliveryManagerAssignmentGroup hubGroup =
                DeliveryManagerAssignmentGroup.hubDelivery(ARRIVAL_HUB_ID);
        stubCompanyManagers(companyGroup, List.of(companyManager(COMPANY_MANAGER_0, 0)));
        stubHubManagers(hubGroup, List.of());

        assertThatThrownBy(() -> assignmentService.assignManagers(
                unassignedPlan(),
                ARRIVAL_HUB_ID
        ))
                .isInstanceOf(DeliveryException.class)
                .extracting(exception -> ((DeliveryException) exception).getErrorCode())
                .isEqualTo(DeliveryErrorCode.DELIVERY_MANAGER_UNAVAILABLE);
    }

    private void stubCompanyManagers(
            DeliveryManagerAssignmentGroup group,
            List<DeliveryManager> managers
    ) {
        when(cursorRepository.acquireForUpdate(group))
                .thenReturn(DeliveryManagerAssignmentCursor.create(group));
        when(deliveryManagerRepository.findActiveManagers(group)).thenReturn(managers);
    }

    private void stubHubManagers(
            DeliveryManagerAssignmentGroup group,
            List<DeliveryManager> managers
    ) {
        when(cursorRepository.acquireForUpdate(group))
                .thenReturn(DeliveryManagerAssignmentCursor.create(group));
        when(deliveryManagerRepository.findActiveManagers(group)).thenReturn(managers);
    }

    private DeliveryManager companyManager(UUID userId, int sequence) {
        return DeliveryManager.create(
                userId,
                DeliveryManagerAssignmentGroup.companyDelivery(ARRIVAL_HUB_ID),
                sequence
        );
    }

    private DeliveryManager hubManager(UUID userId, int sequence) {
        return DeliveryManager.create(
                userId,
                DeliveryManagerAssignmentGroup.hubDelivery(DEPARTURE_HUB_ID),
                sequence
        );
    }

    private DeliveryPlan unassignedPlan() {
        return new DeliveryPlan(null, List.of(route(1, DEPARTURE_HUB_ID, ARRIVAL_HUB_ID)));
    }

    private DeliveryPlan unassignedTwoRoutePlan() {
        return new DeliveryPlan(null, List.of(
                route(1, DEPARTURE_HUB_ID, NEXT_HUB_ID),
                route(2, NEXT_HUB_ID, ARRIVAL_HUB_ID)
        ));
    }

    private DeliveryPlan.Route route(int sequence, UUID departureHubId, UUID arrivalHubId) {
        return new DeliveryPlan.Route(
                sequence,
                departureHubId,
                arrivalHubId,
                new BigDecimal("12.4"),
                30,
                null
        );
    }
}
