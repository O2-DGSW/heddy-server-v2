package com.heddy.global.error;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Set;

/**
 * 모든 에러 응답을 API 명세 §2 포맷 {@code {"error": {...}, "request_id"}}으로 통일한다.
 *
 * <p>파싱·바인딩 실패는 {@code INVALID_REQUEST}(400), 필드 검증 실패는 {@code VALIDATION_FAILED}(422)로 구분한다.
 * 도메인 예외는 {@link ApplicationException}으로 던지면 {@link ErrorCode}가 상태와 코드를 모두 결정한다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();

    @ExceptionHandler(ApplicationException.class)
    ResponseEntity<ApiErrorResponse> handleApplicationException(ApplicationException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return ResponseEntity.status(errorCode.status())
                .body(ApiErrorResponse.of(errorCode, exception.getMessage(), exception.getFieldErrors()));
    }

    /**
     * 메서드 단위 인가({@code @PreAuthorize}) 실패와 서비스가 던진 {@link AccessDeniedException}.
     *
     * <p>이 예외들은 {@code ExceptionTranslationFilter}에 닿기 전에 {@code ExceptionHandlerExceptionResolver}가
     * 먼저 처리하므로 {@link SecurityErrorResponder}가 호출되지 않는다. 필터가 하던 판단을 여기서 그대로 한다 —
     * 익명 사용자면 아직 인증할 기회가 있으니 401, 인증된 사용자면 권한이 없는 것이므로 403.
     */
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException exception) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || trustResolver.isAnonymous(authentication)) {
            log.debug("Access denied for anonymous request", exception);
            return error(CommonErrorCode.AUTHENTICATION_REQUIRED);
        }
        log.debug("Access denied", exception);
        return error(CommonErrorCode.FORBIDDEN_RESOURCE);
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiErrorResponse> handleAuthentication(AuthenticationException exception) {
        log.debug("Authentication failed", exception);
        return error(CommonErrorCode.AUTHENTICATION_REQUIRED);
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

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiErrorResponse> handleNotFound(NoResourceFoundException exception) {
        log.debug("No handler for request", exception);
        return error(CommonErrorCode.RESOURCE_NOT_FOUND);
    }

    /**
     * 존재하는 경로에 지원하지 않는 메서드를 보낸 경우. 404로 내리면 클라이언트가 경로를 의심하게 되므로
     * 405를 유지하고 {@code Allow} 헤더도 그대로 실어 준다. 공통 9종에 405 코드가 없어 코드는 INVALID_REQUEST를 쓴다.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException exception) {
        log.debug("Method not supported", exception);
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED);
        Set<HttpMethod> supported = exception.getSupportedHttpMethods();
        if (supported != null && !supported.isEmpty()) {
            builder.allow(supported.toArray(HttpMethod[]::new));
        }
        return builder.body(ApiErrorResponse.of(CommonErrorCode.INVALID_REQUEST));
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
