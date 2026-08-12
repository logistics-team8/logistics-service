package com.logistics.deliveryservice.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.junit.jupiter.api.Test;

class DeliveryPersistenceConstraintTest {

    @Test
    void mapsOrderIdUniqueConstraintAndSearchIndexes() {
        Table table = Delivery.class.getAnnotation(Table.class);

        assertThat(table.uniqueConstraints())
                .singleElement()
                .satisfies(uniqueConstraint -> {
                    assertThat(uniqueConstraint.name()).isEqualTo("uk_deliveries_order_id");
                    assertThat(uniqueConstraint.columnNames()).containsExactly("order_id");
                });
        assertThat(table.indexes())
                .extracting(Index::name, Index::columnList)
                .containsExactlyInAnyOrder(
                        tuple("idx_deliveries_status", "status"),
                        tuple("idx_deliveries_departure_hub_id", "departure_hub_id"),
                        tuple("idx_deliveries_arrival_hub_id", "arrival_hub_id"),
                        tuple(
                                "idx_deliveries_company_delivery_manager_id",
                                "company_delivery_manager_id"
                        )
                );
    }

    @Test
    void mapsDeliveryAndRouteSequenceUniqueConstraint() {
        Table table = DeliveryRouteHistory.class.getAnnotation(Table.class);
        UniqueConstraint uniqueConstraint = table.uniqueConstraints()[0];

        assertThat(table.uniqueConstraints()).hasSize(1);
        assertThat(uniqueConstraint.name())
                .isEqualTo("uk_delivery_route_histories_delivery_sequence");
        assertThat(uniqueConstraint.columnNames())
                .containsExactly("delivery_id", "route_sequence");
    }
}
