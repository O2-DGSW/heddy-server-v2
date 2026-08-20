package com.heddy.application.account.service;

import com.heddy.domain.account.exception.AccountError;
import com.heddy.domain.account.exception.AccountException;

import java.util.regex.Pattern;

final class PasswordPolicy {

    private static final Pattern LETTER = Pattern.compile("[A-Za-z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");

    private PasswordPolicy() {
    }

    static void validate(String password) {
        if (password == null || password.length() < 8 || password.length() > 100
                || !LETTER.matcher(password).find() || !DIGIT.matcher(password).find()) {
            throw new AccountException(AccountError.WEAK_PASSWORD);
        }
    }
}
