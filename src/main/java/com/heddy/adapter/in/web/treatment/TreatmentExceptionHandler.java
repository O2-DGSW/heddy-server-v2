package com.heddy.adapter.in.web.treatment;

import com.heddy.domain.treatment.exception.TreatmentException;
import com.heddy.global.error.ApiErrorResponse;
import com.heddy.global.filter.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 도메인 불변식 위반을 API 오류로 번역한다. 위반은 모두 요청 값의 문제라 422 로 답하고,
 * 코드·메시지의 단일 출처는 TreatmentError 그 자체라 ErrorCode 로 옮기지 않는다 —
 * 옮기는 순간 같은 문구가 두 곳에 생겨 둘 중 하나는 반드시 뒤처진다.
 */
@RestControllerAdvice
public class TreatmentExceptionHandler {

    @ExceptionHandler(TreatmentException.class)
    ResponseEntity<ApiErrorResponse> handle(TreatmentException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiErrorResponse.of(exception.error().code(), exception.error().message(),
                        RequestIdFilter.get(request)));
    }
}
