package com.logistics.companyproductservice.application.page;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Set;

public final class PageableUtil {
    private static final int DEFAULT_SIZE = 10;
    private static final Set<Integer> ALLOWED_SIZES = Set.of(10, 30, 50);

    private PageableUtil() {
        throw new UnsupportedOperationException("PageableUtil은 인스턴스를 생성할 수 없습니다.");
    }

    public static Pageable normalize(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, DEFAULT_SIZE);
        }

        int normalizedSize = ALLOWED_SIZES.contains(pageable.getPageSize())
                ? pageable.getPageSize()
                : DEFAULT_SIZE;

        return PageRequest.of(pageable.getPageNumber(), normalizedSize, pageable.getSort());
    }
}