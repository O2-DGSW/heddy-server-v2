package com.heddy.adapter.in.web.account.controller;

import com.heddy.domain.account.exception.AccountError;
import com.heddy.domain.account.exception.AccountException;
import com.heddy.global.error.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AccountExceptionHandler {

    @ExceptionHandler(AccountException.class)
    ResponseEntity<ApiErrorResponse> handle(
            AccountException exception,
            HttpServletRequest request
    ) {
        AccountError error = exception.error();
        return ResponseEntity.status(status(error))
                .body(ApiErrorResponse.of(error.code(), error.message(), request.getRequestURI()));
    }

    private HttpStatus status(AccountError error) {
        return switch (error) {
            case LOGIN_ID_DUPLICATED, PHONE_DUPLICATED, SOCIAL_ALREADY_LINKED -> HttpStatus.CONFLICT;
            case LOGIN_FAILED, INVALID_REFRESH_TOKEN, SOCIAL_PENDING_EXPIRED -> HttpStatus.UNAUTHORIZED;
            case ACCOUNT_SUSPENDED, ACCOUNT_INACTIVE -> HttpStatus.FORBIDDEN;
            case PHONE_NOT_VERIFIED, SMS_CODE_INVALID -> HttpStatus.BAD_REQUEST;
            case ACCOUNT_NOT_FOUND, SMS_CODE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case SMS_CODE_MAX_ATTEMPTS -> HttpStatus.LOCKED;
            case SMS_SEND_TOO_SOON -> HttpStatus.TOO_MANY_REQUESTS;
            case SMS_SEND_FAILED -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
