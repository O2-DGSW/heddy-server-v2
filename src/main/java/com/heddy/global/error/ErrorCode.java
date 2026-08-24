package com.heddy.global.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "요청 형식이 올바르지 않습니다."),
    VALIDATION_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_FAILED", "필드 검증에 실패했습니다."),
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "인증이 필요합니다."),
    FORBIDDEN_RESOURCE(HttpStatus.FORBIDDEN, "FORBIDDEN_RESOURCE", "접근 권한이 없습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다."),
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "허용된 파일 크기를 초과했습니다."),
    FILE_CONTENT_TYPE_NOT_ALLOWED(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "FILE_CONTENT_TYPE_NOT_ALLOWED", "허용되지 않는 파일 형식입니다."),
    FILE_CONTENT_TYPE_MISMATCH(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "FILE_CONTENT_TYPE_MISMATCH", "파일 형식이 업로드 세션과 일치하지 않습니다."),
    FILE_OBJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "FILE_OBJECT_NOT_FOUND", "업로드된 객체를 찾을 수 없습니다."),
    FILE_UPLOAD_EXPIRED(HttpStatus.UNPROCESSABLE_ENTITY, "FILE_UPLOAD_EXPIRED", "만료된 업로드 세션입니다."),
    FILE_INVALID_STATE(HttpStatus.CONFLICT, "FILE_INVALID_STATE", "현재 상태에서는 요청한 처리를 할 수 없습니다."),
    FILE_CONCURRENT_MODIFICATION(HttpStatus.CONFLICT, "FILE_CONCURRENT_MODIFICATION", "다른 요청이 파일 상태를 먼저 변경했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }
}
