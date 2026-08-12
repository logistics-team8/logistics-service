package com.logistics.deliveryservice.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.logistics.deliveryservice.domain.model.DeliveryManager;
import com.logistics.deliveryservice.domain.model.DeliveryManagerAssignmentGroup;
import com.logistics.deliveryservice.domain.model.DeliveryManagerType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeliveryManagerRepositoryAdapterTest {

    @Mock
    private DeliveryManagerJpaRepository jpaRepository;

    @InjectMocks
    private DeliveryManagerRepositoryAdapter adapter;

    @Test
    void delegatesSaveAndUserLookupToJpaRepository() {
        UUID userId = UUID.fromString("b64e4c1a-65e4-43fb-a73f-e32ea4fc754e");
        DeliveryManager deliveryManager = mock(DeliveryManager.class);
        when(jpaRepository.saveAndFlush(deliveryManager)).thenReturn(deliveryManager);
        when(jpaRepository.findById(userId)).thenReturn(Optional.of(deliveryManager));

        assertThat(adapter.save(deliveryManager)).isSameAs(deliveryManager);
        assertThat(adapter.findByUserId(userId)).containsSame(deliveryManager);
        verify(jpaRepository).saveAndFlush(deliveryManager);
        verify(jpaRepository).findById(userId);
    }

    @Test
    void delegatesActiveSequenceLookupWithAssignmentGroup() {
        UUID hubId = UUID.fromString("9fd130f7-4c66-4ca4-8db6-f8cb1cbb048f");
        DeliveryManagerAssignmentGroup group =
                DeliveryManagerAssignmentGroup.companyDelivery(hubId);
        when(jpaRepository.findActiveDeliverySequences(
                DeliveryManagerType.COMPANY_DELIVERY,
                hubId
        )).thenReturn(List.of(0, 2));

        assertThat(adapter.findActiveDeliverySequences(group)).containsExactly(0, 2);
        verify(jpaRepository).findActiveDeliverySequences(
                DeliveryManagerType.COMPANY_DELIVERY,
                hubId
        );
    }

    @Test
    void delegatesActiveManagerLookupWithHubIdForCompanyDeliveryGroup() {
        UUID hubId = UUID.fromString("9fd130f7-4c66-4ca4-8db6-f8cb1cbb048f");
        DeliveryManagerAssignmentGroup group =
                DeliveryManagerAssignmentGroup.companyDelivery(hubId);
        DeliveryManager deliveryManager = mock(DeliveryManager.class);
        when(jpaRepository.findActiveManagers(
                DeliveryManagerType.COMPANY_DELIVERY,
                hubId
        )).thenReturn(List.of(deliveryManager));

        assertThat(adapter.findActiveManagers(group)).containsExactly(deliveryManager);
        verify(jpaRepository).findActiveManagers(
                DeliveryManagerType.COMPANY_DELIVERY,
                hubId
        );
    }

    @Test
    void delegatesActiveManagerLookupWithoutHubIdForHubDeliveryGroup() {
        UUID hubId = UUID.fromString("9fd130f7-4c66-4ca4-8db6-f8cb1cbb048f");
        DeliveryManagerAssignmentGroup group =
                DeliveryManagerAssignmentGroup.hubDelivery(hubId);
        DeliveryManager deliveryManager = mock(DeliveryManager.class);
        when(jpaRepository.findActiveManagers(
                DeliveryManagerType.HUB_DELIVERY,
                null
        )).thenReturn(List.of(deliveryManager));

        assertThat(adapter.findActiveManagers(group)).containsExactly(deliveryManager);
        verify(jpaRepository).findActiveManagers(
                DeliveryManagerType.HUB_DELIVERY,
                null
        );
    }
}
