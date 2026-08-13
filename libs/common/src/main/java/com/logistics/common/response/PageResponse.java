package com.logistics.common.response;


import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 공통 페이징 응답 형식입니다.
 *
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
        content = content == null
                ? List.of()
                : List.copyOf(content);
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
