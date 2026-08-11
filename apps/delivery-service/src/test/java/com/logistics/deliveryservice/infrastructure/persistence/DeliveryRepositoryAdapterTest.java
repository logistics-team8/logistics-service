package com.logistics.deliveryservice.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.logistics.deliveryservice.domain.model.Delivery;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeliveryRepositoryAdapterTest {

    @Mock
    private DeliveryJpaRepository jpaRepository;

    @InjectMocks
    private DeliveryRepositoryAdapter adapter;

    @Test
    void delegatesAggregateSaveToJpaRepository() {
        Delivery delivery = mock(Delivery.class);
        when(jpaRepository.saveAndFlush(delivery)).thenReturn(delivery);

        Delivery savedDelivery = adapter.save(delivery);

        assertThat(savedDelivery).isSameAs(delivery);
        verify(jpaRepository).saveAndFlush(delivery);
    }

    @Test
    void delegatesOrderLookupsToJpaRepository() {
        UUID orderId = UUID.fromString("6dc9883c-60da-431b-bc6c-c852ec9c5c1e");
        Delivery delivery = mock(Delivery.class);
        when(jpaRepository.findByOrderId(orderId)).thenReturn(Optional.of(delivery));
        when(jpaRepository.findByOrderIdAndDeletedAtIsNull(orderId))
                .thenReturn(Optional.of(delivery));

        assertThat(adapter.findByOrderId(orderId)).containsSame(delivery);
        assertThat(adapter.findActiveByOrderId(orderId)).containsSame(delivery);
        verify(jpaRepository).findByOrderId(orderId);
        verify(jpaRepository).findByOrderIdAndDeletedAtIsNull(orderId);
    }
}
