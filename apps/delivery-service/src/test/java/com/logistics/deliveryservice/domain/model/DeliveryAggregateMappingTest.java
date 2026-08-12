package com.logistics.deliveryservice.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

// Delivery 애그리거트의 JPA 매핑 및 캡슐화 상태 테스트
class DeliveryAggregateMappingTest {

    /** 엔티티와 실제 DB 테이블명 매핑 검증 테스트 **/
    @Test
    void mapsDeliveryAndRouteHistoryTables() {
        // Delivery 클래스의 @Table 어노테이션에 지정된 name 값이 "p_deliveries"인지 검증
        assertThat(Delivery.class.getAnnotation(Table.class).name())
                .isEqualTo("p_deliveries");

        // DeliveryRouteHistory 클래스의 @Table 어노테이션에 지정된 name 값이 "p_delivery_route_histories"인지 검증합니다.
        assertThat(DeliveryRouteHistory.class.getAnnotation(Table.class).name())
                .isEqualTo("p_delivery_route_histories");
    }

    /** Delivery -> DeliveryRouteHistory (일대다) 영속성 전이(Cascade) 및 고아 객체 삭제 옵션 검증 테스트 **/
    @Test
    void mapsRouteHistoriesWithPersistAndMergeCascadeOnly() throws NoSuchFieldException {
        Field routeHistoriesField = Delivery.class.getDeclaredField("routeHistories");
        OneToMany oneToMany = routeHistoriesField.getAnnotation(OneToMany.class);

        assertThat(oneToMany.mappedBy()).isEqualTo("delivery");
        assertThat(oneToMany.cascade())
                .containsExactlyInAnyOrder(CascadeType.PERSIST, CascadeType.MERGE);
        assertThat(oneToMany.orphanRemoval()).isFalse();
    }

    /** 3. DeliveryRouteHistory -> Delivery (다대일) 외래키(FK) 및 지연 로딩 설정 검증 테스트 **/
    @Test
    void mapsRouteHistoryAsLazyForeignKeyOwner() throws NoSuchFieldException {
        Field deliveryField = DeliveryRouteHistory.class.getDeclaredField("delivery");
        ManyToOne manyToOne = deliveryField.getAnnotation(ManyToOne.class);
        JoinColumn joinColumn = deliveryField.getAnnotation(JoinColumn.class);

        assertThat(manyToOne.fetch()).isEqualTo(FetchType.LAZY);
        assertThat(manyToOne.optional()).isFalse();
        assertThat(joinColumn.name()).isEqualTo("delivery_id");
        assertThat(joinColumn.nullable()).isFalse();
    }

    /** 4. 도메인 캡슐화 검증: 외부에서 획득한 리스트를 무단으로 수정할 수 없는지 테스트 **/
    @Test
    void exposesRouteHistoriesAsReadOnlyCollection() {
        Delivery delivery = new Delivery();

        assertThat(delivery.getRouteHistories()).isEmpty();
        assertThatThrownBy(() -> delivery.getRouteHistories().add(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
