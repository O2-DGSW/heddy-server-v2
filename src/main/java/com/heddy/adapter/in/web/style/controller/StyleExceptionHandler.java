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

import java.util.List;

@RestControllerAdvice
public class StyleExceptionHandler {

    @ExceptionHandler(StyleException.class)
    ResponseEntity<ApiErrorResponse> handle(
            StyleException exception,
            HttpServletRequest request
    ) {
        StyleError error = exception.error();
        ApiErrorResponse response = error == StyleError.PREFERENCE_CONFLICT
                ? conflictResponse(error, RequestIdFilter.get(request))
                : ApiErrorResponse.of(
                        error.code(), error.message(), RequestIdFilter.get(request));
        return ResponseEntity.status(status(error)).body(response);
    }

    private ApiErrorResponse conflictResponse(StyleError error, String requestId) {
        return new ApiErrorResponse(
                new ApiErrorResponse.ErrorBody(error.code(), error.message(), List.of(
                        new ApiErrorResponse.FieldError(
                                "preferred_tag_ids", "DUPLICATED_WITH_EXCLUDED_TAGS"))),
                requestId);
    }

    private HttpStatus status(StyleError error) {
        return switch (error) {
            case PREFERENCE_LIMIT_EXCEEDED -> HttpStatus.UNPROCESSABLE_CONTENT;
            case PREFERENCE_CONFLICT -> HttpStatus.CONFLICT;
            case TAG_NOT_FOUND -> HttpStatus.NOT_FOUND;
        };
    }
}
