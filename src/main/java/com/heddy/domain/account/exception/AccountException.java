package com.heddy.domain.account.exception;

public class AccountException extends RuntimeException {

    private final AccountError error;

    public AccountException(AccountError error) {
        super(error.message());
        this.error = error;
    }

    public AccountError error() {
        return error;
    }
}
