package com.heddy.global.error;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ApiErrorResponse(
        ErrorBody error,
        @JsonProperty("request_id") String requestId
) {
    public record ErrorBody(
            String code,
            String message,
            @JsonProperty("field_errors") List<FieldError> fieldErrors
    ) {
    }

    public record FieldError(String field, String reason) {
    }

    public static ApiErrorResponse of(String code, String message, String requestId) {
        return new ApiErrorResponse(new ErrorBody(code, message, List.of()), requestId);
    }

    public static ApiErrorResponse validation(String requestId, List<FieldError> errors) {
        return new ApiErrorResponse(
                new ErrorBody(ErrorCode.VALIDATION_FAILED.code(),
                        ErrorCode.VALIDATION_FAILED.message(), errors),
                requestId);
    }
}
