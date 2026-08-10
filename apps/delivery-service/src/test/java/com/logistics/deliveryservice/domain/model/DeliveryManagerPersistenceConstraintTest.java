package com.logistics.deliveryservice.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.logistics.deliveryservice.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class DeliveryManagerPersistenceConstraintTest {

    @Test
    void mapsUserIdAsImmutablePrimaryKeyAndGroupIndex() throws NoSuchFieldException {
        Table table = DeliveryManager.class.getAnnotation(Table.class);
        Field userIdField = DeliveryManager.class.getDeclaredField("userId");
        Column userIdColumn = userIdField.getAnnotation(Column.class);

        assertThat(table.name()).isEqualTo("p_delivery_managers");
        assertThat(table.indexes())
                .extracting(Index::name, Index::columnList)
                .containsExactly(tuple(
                        "idx_delivery_managers_group_sequence",
                        "manager_type, hub_id, delivery_sequence"
                ));
        assertThat(userIdField.isAnnotationPresent(Id.class)).isTrue();
        assertThat(userIdColumn.name()).isEqualTo("user_id");
        assertThat(userIdColumn.updatable()).isFalse();
        assertThat(BaseEntity.class.isAssignableFrom(DeliveryManager.class)).isTrue();
    }

    @Test
    void mapsCursorGroupKeyUniqueConstraint() {
        Table table = DeliveryManagerAssignmentCursor.class.getAnnotation(Table.class);

        assertThat(table.name()).isEqualTo("p_delivery_manager_assignment_cursors");
        assertThat(table.uniqueConstraints()).singleElement().satisfies(constraint -> {
            assertThat(constraint.name())
                    .isEqualTo("uk_delivery_manager_assignment_cursors_group_key");
            assertThat(constraint.columnNames())
                    .containsExactly("assignment_group_key");
        });
        assertThat(BaseEntity.class.isAssignableFrom(
                DeliveryManagerAssignmentCursor.class
        )).isTrue();
    }
}
