package com.logistics.orderservice.page;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Set;

/**
 * <p>허용하는 페이지 크기는 10, 30, 50입니다.</p>
 *
 * <p>허용되지 않은 크기가 전달되면 기본값인 10으로 보정합니다.</p>
 *
 * <pre>
 * page=2&size=25&sort=createdAt,desc
 * → page=2&size=10&sort=createdAt,desc
 * </pre>
 */
public final class PageableUtil {
    private static final int DEFAULT_SIZE = 10;

    private static final Set<Integer> ALLOWED_SIZES =
            Set.of(10, 30, 50);

    private PageableUtil() {
        throw new UnsupportedOperationException(
                "PageableUtil은 인스턴스를 생성할 수 없습니다."
        );
    }

    public static Pageable normalize(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, DEFAULT_SIZE);
        }

        int normalizedSize = ALLOWED_SIZES.contains(pageable.getPageSize())
                ? pageable.getPageSize()
                : DEFAULT_SIZE;

        return PageRequest.of(
                pageable.getPageNumber(),
                normalizedSize,
                pageable.getSort()
        );
    }
}
