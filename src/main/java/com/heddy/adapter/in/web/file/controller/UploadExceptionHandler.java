package com.heddy.adapter.in.web.file.controller;

import com.heddy.domain.file.exception.FileError;
import com.heddy.domain.file.exception.FileException;
import com.heddy.global.error.ApiErrorResponse;
import com.heddy.global.error.ErrorCode;
import com.heddy.global.filter.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class UploadExceptionHandler {

    /**
     * 도메인 예외를 API 오류 코드로 번역한다. 상태 코드는 ErrorCode 가, 코드·메시지 문자열도
     * ErrorCode 가 단일 출처다. FileError 와 ErrorCode 의 대응이 어긋나면 컴파일이 막는다.
     */
    @ExceptionHandler(FileException.class)
    ResponseEntity<ApiErrorResponse> handle(FileException exception, HttpServletRequest request) {
        ErrorCode errorCode = errorCodeOf(exception.error());
        return ResponseEntity.status(errorCode.status())
                .body(ApiErrorResponse.of(
                        errorCode.code(), errorCode.message(), RequestIdFilter.get(request)));
    }

    private static ErrorCode errorCodeOf(FileError error) {
        return switch (error) {
            case TOO_LARGE -> ErrorCode.FILE_TOO_LARGE;
            case CONTENT_TYPE_NOT_ALLOWED -> ErrorCode.FILE_CONTENT_TYPE_NOT_ALLOWED;
            case PURPOSE_NOT_ALLOWED -> ErrorCode.FILE_PURPOSE_NOT_ALLOWED;
            case CONTENT_TYPE_MISMATCH -> ErrorCode.FILE_CONTENT_TYPE_MISMATCH;
            case SIZE_MISMATCH -> ErrorCode.FILE_SIZE_MISMATCH;
            case OBJECT_NOT_FOUND -> ErrorCode.FILE_OBJECT_NOT_FOUND;
            case UPLOAD_EXPIRED -> ErrorCode.FILE_UPLOAD_EXPIRED;
            case INVALID_STATE_TRANSITION -> ErrorCode.FILE_INVALID_STATE;
            case CONCURRENT_MODIFICATION -> ErrorCode.FILE_CONCURRENT_MODIFICATION;
        };
    }
}
