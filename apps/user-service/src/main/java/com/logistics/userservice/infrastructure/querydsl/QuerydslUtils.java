package com.logistics.userservice.infrastructure.querydsl;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.*;
import lombok.experimental.UtilityClass;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;

/** QueryDSL 유틸리티 클래스 */
@UtilityClass
public class QuerydslUtils {

    /**
     * 단일 값 비교
     *
     * @param data
     * @param value
     * @return
     * @param <T>
     */
    public static <T> BooleanExpression eq(SimpleExpression<T> data, T value) {
        return value == null ? null : data.eq(value);
    }

    /**
     * 여러 값 비교
     *
     * @param data
     * @param values
     * @return
     * @param <V>
     */
    public static <V> BooleanExpression in(
            SimpleExpression<V> data, Collection<? extends V> values) {
        return (values == null || values.isEmpty()) ? null : data.in(values);
    }

    /**
     * Collection / String 값 비교
     *
     * @param data
     * @param value
     * @return
     * @param <V>
     */
    public static <V> BooleanExpression contains(CollectionPathBase<?, V, ?> data, V value) {
        return value == null ? null : data.contains(value);
    }

    public static BooleanExpression startsWith(StringPath data, String value) {
        return value == null || value.isBlank() ? null : data.startsWith(value);
    }

    /**
     * 검색 기간
     *
     * @param data
     * @param startDate
     * @param endDate
     * @return
     */
    public static BooleanExpression createdAtBetween(
            DateTimePath<LocalDateTime> data, LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null) {
            return data.between(startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
        }

        if (startDate != null) {
            return data.goe(startDate.atStartOfDay());
        }

        if (endDate != null) {
            return data.loe(endDate.atTime(LocalTime.MAX));
        }
        return null;
    }

    /**
     * 정렬조건
     *
     * @param pageable
     * @param qClass
     * @return
     * @param <T>
     */
    public static <T> OrderSpecifier<?>[] getSort(Pageable pageable, EntityPathBase<T> qClass) {

        PathBuilder<T> pathBuilder = new PathBuilder<>(qClass.getType(), qClass.getMetadata());

        return pageable.getSort().stream()
                .map(
                        order ->
                                new OrderSpecifier<>(
                                        order.isAscending() ? Order.ASC : Order.DESC,
                                        pathBuilder.getString(order.getProperty())))
                .toArray(OrderSpecifier[]::new);
    }
}
