package com.heddy.global.error;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        String code,
        String message,
        String path,
        Instant timestamp,
        List<FieldError> errors
) {
    public record FieldError(String field, String reason) {
    }

    public static ApiErrorResponse of(ErrorCode errorCode, String message, String path) {
        return new ApiErrorResponse(errorCode.code(), message, path, Instant.now(), List.of());
    }

    public static ApiErrorResponse of(String code, String message, String path) {
        return new ApiErrorResponse(code, message, path, Instant.now(), List.of());
    }

    public static ApiErrorResponse validation(String path, List<FieldError> errors) {
        return new ApiErrorResponse(
                ErrorCode.INVALID_INPUT.code(),
                ErrorCode.INVALID_INPUT.message(),
                path,
                Instant.now(),
                errors);
    }
}
