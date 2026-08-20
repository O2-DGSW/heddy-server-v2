package com.heddy.global.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.heddy.global.filter.RequestIdFilter;

import java.util.List;

/**
 * API 명세 2절 공통 에러 응답. {@code {"error": {"code", "message", "field_errors"}, "request_id"}} 형태다.
 */
public record ApiErrorResponse(Error error, String requestId) {

    /**
     * {@code code}·{@code message}는 명세상 필수라 비어 있어도 키를 남긴다.
     * 클래스 레벨에 NON_EMPTY를 걸면 이 둘까지 대상이 되므로 생략 규칙은 {@code fieldErrors}에만 건다.
     * {@code message}에 ALWAYS를 명시한 건 전역 설정({@code default-property-inclusion: non_null})이
     * null 메시지에서 키를 지우는 것까지 막기 위해서다.
     */
    public record Error(
            String code,
            @JsonInclude(JsonInclude.Include.ALWAYS) String message,
            @JsonInclude(JsonInclude.Include.NON_EMPTY) List<FieldError> fieldErrors
    ) {
    }

    public record FieldError(String field, String reason) {
    }

    public static ApiErrorResponse of(ErrorCode errorCode) {
        return of(errorCode, errorCode.message());
    }

    public static ApiErrorResponse of(ErrorCode errorCode, String message) {
        return of(errorCode, message, List.of());
    }

    public static ApiErrorResponse of(ErrorCode errorCode, String message, List<FieldError> fieldErrors) {
        return new ApiErrorResponse(
                new Error(errorCode.code(), message, fieldErrors),
                RequestIdFilter.currentRequestId());
    }
}
