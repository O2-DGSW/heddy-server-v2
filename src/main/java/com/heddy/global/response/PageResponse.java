package com.heddy.global.response;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * API 명세 2.5절 공통 페이지네이션 응답. {@code {"items": [...], "page": {...}}} 형태로 직렬화된다.
 */
public record PageResponse<T>(List<T> items, PageMeta page) {

    public record PageMeta(
            int number,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext
    ) {
    }

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page.getContent(), meta(page));
    }

    /** 엔티티 페이지를 응답 DTO로 변환하면서 감싼다. */
    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(page.getContent().stream().map(mapper).toList(), meta(page));
    }

    private static PageMeta meta(Page<?> page) {
        return new PageMeta(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext());
    }
}
