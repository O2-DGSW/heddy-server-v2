package com.heddy.global.error;

import java.util.List;

/**
 * 도메인·서비스 계층이 던지는 공통 예외. {@link ErrorCode}가 HTTP 상태와 응답 코드 문자열을 모두 결정한다.
 */
public class ApplicationException extends RuntimeException {

    private final ErrorCode errorCode;
    private final List<ApiErrorResponse.FieldError> fieldErrors;

    public ApplicationException(ErrorCode errorCode) {
        this(errorCode, errorCode.message(), List.of());
    }

    public ApplicationException(ErrorCode errorCode, String message) {
        this(errorCode, message, List.of());
    }

    public ApplicationException(ErrorCode errorCode, String message, List<ApiErrorResponse.FieldError> fieldErrors) {
        super(message);
        this.errorCode = errorCode;
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public List<ApiErrorResponse.FieldError> getFieldErrors() {
        return fieldErrors;
    }
}
