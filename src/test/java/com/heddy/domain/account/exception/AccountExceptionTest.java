package com.heddy.domain.account.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccountExceptionTest {

    @Test
    void preservesCauseWhenGiven() {
        RuntimeException cause = new RuntimeException("solapi 오류");

        AccountException exception = new AccountException(AccountError.SMS_SEND_FAILED, cause);

        assertThat(exception.getCause()).isSameAs(cause);
        assertThat(exception.error()).isEqualTo(AccountError.SMS_SEND_FAILED);
    }

    @Test
    void hasNoCauseWhenNotGiven() {
        AccountException exception = new AccountException(AccountError.SMS_SEND_FAILED);

        assertThat(exception.getCause()).isNull();
        assertThat(exception.error()).isEqualTo(AccountError.SMS_SEND_FAILED);
    }
}
