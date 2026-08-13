package com.logistics.common.response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.logistics.common.error.CommonErrorCode;
import com.logistics.common.exception.BusinessException;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@DisplayName("공통 Pageable 정책")
class PageableUtilTest {

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of("createdAt", "updatedAt");

    @Test
    @DisplayName("Pageable이 없으면 0페이지와 크기 10을 기본값으로 사용한다")
    void normalizeUsesDefaultsWhenPageableIsNull() {
        Pageable pageable = PageableUtil.normalize(null);

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort().isUnsorted()).isTrue();
    }

    @DisplayName("페이지 크기 10, 30, 50과 요청한 정렬 조건을 유지한다")
    @ParameterizedTest(name = "size={0}")
    @ValueSource(ints = {10, 30, 50})
    void normalizeKeepsAllowedPageSizeAndSort(int requestedSize) {
        Pageable requested = PageRequest.of(2, requestedSize, Sort.by(Sort.Direction.ASC, "updatedAt"));

        Pageable pageable = PageableUtil.normalize(requested, ALLOWED_SORT_PROPERTIES);

        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(requestedSize);
        assertThat(pageable.getSort().getOrderFor("updatedAt")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("updatedAt").isAscending()).isTrue();
    }

    @Test
    @DisplayName("허용하지 않는 페이지 크기는 10으로 보정한다")
    void normalizeFallsBackToTenForUnsupportedPageSize() {
        Pageable requested = PageRequest.of(1, 25, Sort.by(Sort.Direction.DESC, "createdAt"));

        Pageable pageable = PageableUtil.normalize(requested, ALLOWED_SORT_PROPERTIES);

        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort().getOrderFor("createdAt").isDescending()).isTrue();
    }

    @Test
    @DisplayName("허용하지 않는 정렬 필드는 잘못된 요청으로 거절한다")
    void normalizeRejectsUnsupportedSortProperty() {
        Pageable requested = PageRequest.of(0, 10, Sort.by("name"));

        assertThatThrownBy(() -> PageableUtil.normalize(requested, ALLOWED_SORT_PROPERTIES))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(CommonErrorCode.INVALID_INPUT));
    }
}
