package com.logistics.deliveryservice.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.logistics.deliveryservice.domain.model.Delivery;
import org.junit.jupiter.api.Test;

class DeliveryRepositoryAdapterTest {

    @Test
    void delegatesAggregateSaveToJpaRepository() {
        DeliveryJpaRepository jpaRepository = mock(DeliveryJpaRepository.class);
        DeliveryRepositoryAdapter adapter = new DeliveryRepositoryAdapter(jpaRepository);
        Delivery delivery = mock(Delivery.class);
        when(jpaRepository.save(delivery)).thenReturn(delivery);

        Delivery savedDelivery = adapter.save(delivery);

        assertThat(savedDelivery).isSameAs(delivery);
        verify(jpaRepository).save(delivery);
    }
}
