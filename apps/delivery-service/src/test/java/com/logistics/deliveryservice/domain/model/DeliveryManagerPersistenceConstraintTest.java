package com.logistics.deliveryservice.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.deliveryservice.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class DeliveryManagerPersistenceConstraintTest {

    @Test
    void mapsUserIdAsImmutablePrimaryKeyWithoutUndocumentedIndex() throws NoSuchFieldException {
        Table table = DeliveryManager.class.getAnnotation(Table.class);
        Field userIdField = DeliveryManager.class.getDeclaredField("userId");
        Column userIdColumn = userIdField.getAnnotation(Column.class);

        assertThat(table.name()).isEqualTo("p_delivery_managers");
        assertThat(table.indexes()).isEmpty();
        assertThat(userIdField.isAnnotationPresent(Id.class)).isTrue();
        assertThat(userIdColumn.name()).isEqualTo("user_id");
        assertThat(userIdColumn.updatable()).isFalse();
        assertThat(BaseEntity.class.isAssignableFrom(DeliveryManager.class)).isTrue();
    }
}
