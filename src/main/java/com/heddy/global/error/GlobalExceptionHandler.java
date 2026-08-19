package com.heddy.global.error;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * 모든 에러 응답을 API 명세 §2 포맷 {@code {"error": {...}, "request_id"}}으로 통일한다.
 *
 * <p>파싱·바인딩 실패는 {@code INVALID_REQUEST}(400), 필드 검증 실패는 {@code VALIDATION_FAILED}(422)로 구분한다.
 * 도메인 예외는 {@link ApplicationException}으로 던지면 {@link ErrorCode}가 상태와 코드를 모두 결정한다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApplicationException.class)
    ResponseEntity<ApiErrorResponse> handleApplicationException(ApplicationException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return ResponseEntity.status(errorCode.status())
                .body(ApiErrorResponse.of(errorCode, exception.getMessage(), exception.getFieldErrors()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        return validationFailed(exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiErrorResponse.FieldError(error.getField(), error.getDefaultMessage()))
                .toList());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
        return validationFailed(exception.getConstraintViolations().stream()
                .map(violation -> new ApiErrorResponse.FieldError(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()))
                .toList());
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    ResponseEntity<ApiErrorResponse> handleMalformedRequest(Exception exception) {
        log.debug("Malformed request", exception);
        return error(CommonErrorCode.INVALID_REQUEST);
    }

    @ExceptionHandler({NoResourceFoundException.class, HttpRequestMethodNotSupportedException.class})
    ResponseEntity<ApiErrorResponse> handleNotFound(Exception exception) {
        log.debug("No handler for request", exception);
        return error(CommonErrorCode.RESOURCE_NOT_FOUND);
    }

    /**
     * 요청 Content-Type 불일치. 업로드 이미지 형식 오류({@code UPLOAD_MEDIA_TYPE_UNSUPPORTED})는
     * 업로드 도메인이 자기 코드로 따로 다룬다. 여기서는 공통 코드로 내리되 상태만 415를 유지한다.
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException exception) {
        log.debug("Unsupported media type", exception);
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ApiErrorResponse.of(CommonErrorCode.INVALID_REQUEST));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception) {
        log.error("Unexpected error", exception);
        return error(CommonErrorCode.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ApiErrorResponse> validationFailed(List<ApiErrorResponse.FieldError> fieldErrors) {
        ErrorCode errorCode = CommonErrorCode.VALIDATION_FAILED;
        return ResponseEntity.status(errorCode.status())
                .body(ApiErrorResponse.of(errorCode, errorCode.message(), fieldErrors));
    }

    private ResponseEntity<ApiErrorResponse> error(ErrorCode errorCode) {
        return ResponseEntity.status(errorCode.status()).body(ApiErrorResponse.of(errorCode));
    }
}
