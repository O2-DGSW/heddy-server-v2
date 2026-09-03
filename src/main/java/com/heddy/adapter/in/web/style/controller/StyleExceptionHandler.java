package com.heddy.adapter.in.web.style.controller;

import com.heddy.domain.style.exception.StyleError;
import com.heddy.domain.style.exception.StyleException;
import com.heddy.global.error.ApiErrorResponse;
import com.heddy.global.filter.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestControllerAdvice
public class StyleExceptionHandler {

    @ExceptionHandler(StyleException.class)
    ResponseEntity<ApiErrorResponse> handle(
            StyleException exception,
            HttpServletRequest request
    ) {
        StyleError error = exception.error();
        String requestId = RequestIdFilter.get(request);
        ApiErrorResponse response = switch (error) {
            case PREFERENCE_CONFLICT -> conflictResponse(error, requestId);
            case INVALID_TAG_IDS -> invalidTagIdsResponse(exception, requestId);
            case PREFERENCE_LIMIT_EXCEEDED, SAVED_STYLE_DUPLICATED,
                    SAVED_STYLE_LIMIT_EXCEEDED ->
                    ApiErrorResponse.of(error.code(), error.message(), requestId);
        };
        return ResponseEntity.status(status(error)).body(response);
    }

    private ApiErrorResponse conflictResponse(StyleError error, String requestId) {
        return new ApiErrorResponse(
                new ApiErrorResponse.ErrorBody(error.code(), error.message(), List.of(
                        new ApiErrorResponse.FieldError(
                                "preferred_tag_ids", "DUPLICATED_WITH_EXCLUDED_TAGS"))),
                requestId);
    }

    private ApiErrorResponse invalidTagIdsResponse(
            StyleException exception,
            String requestId
    ) {
        List<ApiErrorResponse.FieldError> fieldErrors = new ArrayList<>();
        addInvalidTagErrors(
                fieldErrors, "preferred_tag_ids", exception.invalidPreferredTagIds());
        addInvalidTagErrors(
                fieldErrors, "excluded_tag_ids", exception.invalidExcludedTagIds());
        return ApiErrorResponse.validation(requestId, fieldErrors);
    }

    private void addInvalidTagErrors(
            List<ApiErrorResponse.FieldError> fieldErrors,
            String field,
            List<UUID> invalidTagIds
    ) {
        invalidTagIds.forEach(tagId -> fieldErrors.add(
                new ApiErrorResponse.FieldError(
                        field, "STYLE_TAG_NOT_FOUND:" + tagId)));
    }

    private HttpStatus status(StyleError error) {
        return switch (error) {
            case PREFERENCE_LIMIT_EXCEEDED, INVALID_TAG_IDS, SAVED_STYLE_LIMIT_EXCEEDED ->
                    HttpStatus.UNPROCESSABLE_CONTENT;
            case PREFERENCE_CONFLICT, SAVED_STYLE_DUPLICATED -> HttpStatus.CONFLICT;
        };
    }
}
