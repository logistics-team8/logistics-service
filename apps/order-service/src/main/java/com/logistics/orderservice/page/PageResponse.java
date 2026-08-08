package com.logistics.orderservice.page;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * <pre>
 * {
 *   "content": [],
 *   "page": 0,
 *   "size": 10,
 *   "totalElements": 0,
 *   "totalPages": 0,
 *   "hasNext": false
 * }
 * </pre>
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
    public PageResponse {
        content = List.copyOf(content);
    }

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }
}
