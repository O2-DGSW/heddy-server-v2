package com.heddy.global.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** 목록 API가 공통으로 사용하는 페이지 응답 본문. */
public record PageResponse<T>(
        List<T> items,
        Page page
) {
    public PageResponse {
        items = List.copyOf(items);
    }

    public static <T> PageResponse<T> of(
            List<T> items,
            int number,
            int size,
            long totalElements
    ) {
        long totalPages = totalElements == 0 ? 0 : (totalElements + size - 1) / size;
        return new PageResponse<>(items,
                new Page(number, size, totalElements, totalPages, number + 1L < totalPages));
    }

    public record Page(
            int number,
            int size,
            @JsonProperty("total_elements") long totalElements,
            @JsonProperty("total_pages") long totalPages,
            @JsonProperty("has_next") boolean hasNext
    ) {
    }
}
