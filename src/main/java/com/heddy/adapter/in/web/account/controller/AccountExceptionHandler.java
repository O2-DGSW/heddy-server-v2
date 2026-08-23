package com.heddy.adapter.in.web.account.controller;

import com.heddy.domain.account.exception.AccountError;
import com.heddy.domain.account.exception.AccountException;
import com.heddy.global.error.ApiErrorResponse;
import com.heddy.global.filter.RequestIdFilter;
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
                .body(ApiErrorResponse.of(
                        error.code(), error.message(), RequestIdFilter.get(request)));
    }

    private HttpStatus status(AccountError error) {
        return switch (error) {
            case EMAIL_ALREADY_EXISTS, SOCIAL_ACCOUNT_ALREADY_LINKED, PHONE_ALREADY_EXISTS ->
                    HttpStatus.CONFLICT;
            case INVALID_CREDENTIALS, SOCIAL_TOKEN_INVALID, REFRESH_TOKEN_INVALID,
                    REFRESH_TOKEN_REUSED, REAUTHENTICATION_REQUIRED -> HttpStatus.UNAUTHORIZED;
            case ACCOUNT_LOCKED, SMS_CODE_MAX_ATTEMPTS -> HttpStatus.LOCKED;
            case ACCOUNT_DELETED -> HttpStatus.FORBIDDEN;
            case WEAK_PASSWORD, CONSENT_REQUIRED_NOT_GRANTED,
                    CONSENT_POLICY_VERSION_INVALID, REQUIRED_CONSENT_WITHDRAWAL,
                    PROFILE_INVALID_NICKNAME,
                    PROFILE_PHONE_REQUIRED,
                    PHONE_NOT_VERIFIED,
                    SMS_CODE_INVALID -> HttpStatus.UNPROCESSABLE_ENTITY;
            case ACCOUNT_NOT_FOUND, HAIR_PROFILE_NOT_FOUND, SMS_CODE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case SMS_SEND_TOO_SOON -> HttpStatus.TOO_MANY_REQUESTS;
            case SMS_SEND_FAILED -> HttpStatus.SERVICE_UNAVAILABLE;
        };
    }
}
