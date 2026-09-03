package com.heddy.global.error;

import com.heddy.global.filter.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApplicationException.class)
    ResponseEntity<ApiErrorResponse> handleApplicationException(
            ApplicationException exception,
            HttpServletRequest request
    ) {
        ErrorCode errorCode = exception.getErrorCode();
        return ResponseEntity.status(errorCode.status())
                .body(ApiErrorResponse.of(
                        errorCode.code(), exception.getMessage(), RequestIdFilter.get(request)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<ApiErrorResponse.FieldError> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiErrorResponse.FieldError(
                        snakeCase(error.getField()), error.getDefaultMessage()))
                .toList();
        return ResponseEntity.unprocessableEntity()
                .body(ApiErrorResponse.validation(RequestIdFilter.get(request), errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        List<ApiErrorResponse.FieldError> errors = exception.getConstraintViolations().stream()
                .map(violation -> new ApiErrorResponse.FieldError(
                        snakeCase(violation.getPropertyPath().toString()), violation.getMessage()))
                .toList();
        return ResponseEntity.unprocessableEntity()
                .body(ApiErrorResponse.validation(RequestIdFilter.get(request), errors));
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    ResponseEntity<ApiErrorResponse> handleInvalidRequest(Exception exception, HttpServletRequest request) {
        ErrorCode errorCode = ErrorCode.INVALID_REQUEST;
        return ResponseEntity.status(errorCode.status())
                .body(ApiErrorResponse.of(
                        errorCode.code(), errorCode.message(), RequestIdFilter.get(request)));
    }

    /**
     * 매칭되는 핸들러가 없는 요청. 아래 {@code Exception} 핸들러가 함께 삼키면 경로 오타가
     * 500 으로 보고돼 클라이언트는 재시도할 상황인지 요청을 고칠 상황인지 구분할 수 없고,
     * 오류 로그도 이 소음에 묻힌다. 서버가 아니라 요청의 문제이므로 로그를 남기지 않는다.
     *
     * <p>정적 리소스 핸들러가 있으면 {@code NoResourceFoundException} 이,
     * {@code throw-exception-if-no-handler-found} 로 동작하면
     * {@code NoHandlerFoundException} 이 올라온다. 설정에 기대지 않도록 둘 다 받는다.
     */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    ResponseEntity<ApiErrorResponse> handleNotFound(Exception exception, HttpServletRequest request) {
        ErrorCode errorCode = ErrorCode.RESOURCE_NOT_FOUND;
        return ResponseEntity.status(errorCode.status())
                .body(ApiErrorResponse.of(
                        errorCode.code(), errorCode.message(), RequestIdFilter.get(request)));
    }

    /** 경로는 있으나 메서드가 없는 경우. 스프링이 계산해 둔 Allow 헤더를 그대로 실어 보낸다. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiErrorResponse> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request
    ) {
        ErrorCode errorCode = ErrorCode.METHOD_NOT_ALLOWED;
        return ResponseEntity.status(errorCode.status())
                .headers(exception.getHeaders())
                .body(ApiErrorResponse.of(
                        errorCode.code(), errorCode.message(), RequestIdFilter.get(request)));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Unexpected error", exception);
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(errorCode.status())
                .body(ApiErrorResponse.of(
                        errorCode.code(), errorCode.message(), RequestIdFilter.get(request)));
    }

    private String snakeCase(String value) {
        return value.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }
}
