package com.heddy.adapter.in.web.sharing;

import com.heddy.domain.sharing.exception.SharingError;
import com.heddy.domain.sharing.exception.SharingException;
import com.heddy.global.error.ApiErrorResponse;
import com.heddy.global.filter.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 공유 도메인 예외를 API 오류로 번역한다. 선택 규칙 위반은 요청 값의 문제라 422 로 답하고,
 * 코드·메시지의 단일 출처는 SharingError 그 자체라 ErrorCode 로 옮기지 않는다 — 옮기는 순간
 * 같은 문구가 두 곳에 생겨 둘 중 하나는 반드시 뒤처진다.
 *
 * <p>무인증 공개 조회의 오류(토큰 불일치·만료·철회)는 인증된 소유자 오류보다 노출 범위가
 * 넓으므로, 성공 응답과 마찬가지로 캐시·색인 금지 헤더를 단다.
 */
@RestControllerAdvice
public class SharingExceptionHandler {

    @ExceptionHandler(SharingException.class)
    ResponseEntity<ApiErrorResponse> handle(SharingException exception, HttpServletRequest request) {
        SharingError error = exception.error();
        HttpStatus status = error == SharingError.TOKEN_INVALID
                ? HttpStatus.NOT_FOUND
                : HttpStatus.UNPROCESSABLE_ENTITY;
        ResponseEntity.BodyBuilder response = ResponseEntity.status(status);
        if (isPublicAccessError(error)) {
            response.header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                    .header("X-Robots-Tag", "noindex, nofollow");
        }
        return response.body(ApiErrorResponse.of(error.code(), error.message(),
                RequestIdFilter.get(request)));
    }

    private boolean isPublicAccessError(SharingError error) {
        return error == SharingError.TOKEN_INVALID
                || error == SharingError.EXPIRED
                || error == SharingError.REVOKED;
    }
}

