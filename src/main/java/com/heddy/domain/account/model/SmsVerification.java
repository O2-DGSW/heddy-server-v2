package com.heddy.domain.account.model;

import java.time.Instant;

public record SmsVerification(String code, int attempts, Instant createdAt) {

    public SmsVerification incrementAttempts() {
        return new SmsVerification(code, attempts + 1, createdAt);
    }
}
