package com.heddy.adapter.in.web.recommendation;

import com.heddy.domain.recommendation.exception.RecommendationError;
import com.heddy.domain.recommendation.exception.RecommendationException;
import com.heddy.global.error.ApiErrorResponse;
import com.heddy.global.filter.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RecommendationExceptionHandler {
    @ExceptionHandler(RecommendationException.class)
    ResponseEntity<ApiErrorResponse> handle(
            RecommendationException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = exception.error() == RecommendationError.NOT_FOUND
                ? HttpStatus.NOT_FOUND : HttpStatus.UNPROCESSABLE_CONTENT;
        return ResponseEntity.status(status).body(ApiErrorResponse.of(
                exception.error().code(), exception.error().message(), RequestIdFilter.get(request)));
    }
}
