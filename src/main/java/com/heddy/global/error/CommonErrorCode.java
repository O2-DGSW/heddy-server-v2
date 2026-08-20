package com.heddy.global.error;

import org.springframework.http.HttpStatus;

/**
 * API 명세 18.1절 공통 오류 코드 9종. 도메인에 종속되지 않는 코드만 여기에 둔다.
 */
public enum CommonErrorCode implements ErrorCode {

    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 형식이 올바르지 않습니다."),
    VALIDATION_FAILED(HttpStatus.UNPROCESSABLE_CONTENT, "요청 필드 검증에 실패했습니다."),
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    ACCESS_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Access Token이 만료되었습니다."),
    FORBIDDEN_RESOURCE(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "요청 제한을 초과했습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),
    DEPENDENCY_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "외부 의존성이 일시적으로 응답하지 않습니다.");

    private final HttpStatus status;
    private final String message;

    CommonErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override
    public HttpStatus status() {
        return status;
    }

    @Override
    public String message() {
        return message;
    }
}
