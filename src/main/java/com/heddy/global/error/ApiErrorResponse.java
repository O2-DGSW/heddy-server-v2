package com.heddy.global.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.heddy.global.filter.RequestIdFilter;

import java.util.List;

/**
 * API 명세 §2 공통 에러 응답. {@code {"error": {"code", "message", "field_errors"}, "request_id"}} 형태다.
 */
public record ApiErrorResponse(Error error, String requestId) {

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record Error(String code, String message, List<FieldError> fieldErrors) {
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
