package com.heddy.global.response;

import com.heddy.global.filter.RequestIdFilter;

/**
 * API 명세 2절 공통 성공 응답 래퍼. 모든 성공 응답은 {@code {"data": ..., "request_id": "..."}} 형태다.
 *
 * @param data      실제 응답 본문. 본문이 없는 응답(204 등)은 이 래퍼를 쓰지 않는다.
 * @param requestId 요청 추적 ID. {@link RequestIdFilter}가 MDC에 넣은 값을 그대로 싣는다.
 */
public record ApiResponse<T>(T data, String requestId) {

    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data, RequestIdFilter.currentRequestId());
    }
}
