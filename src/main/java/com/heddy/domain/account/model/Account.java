package com.heddy.domain.account.model;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public record Account(
        UUID userId,
        String email,
        String passwordHash,
        AuthProvider authProvider,
        String providerSubject,
        AccountStatus status,
        int loginFailCount,
        Instant lockedUntil,
        Instant createdAt,
        Instant updatedAt
) {
    public Account(
            UUID userId,
            String email,
            String passwordHash,
            AuthProvider authProvider,
            String providerSubject,
            AccountStatus status,
            int loginFailCount,
            Instant lockedUntil
    ) {
        this(userId, email, passwordHash, authProvider, providerSubject, status,
                loginFailCount, lockedUntil, null, null);
    }

    public static Account email(UUID userId, String email, String passwordHash) {
        return new Account(userId, email, passwordHash, AuthProvider.EMAIL, null,
                AccountStatus.ACTIVE, 0, null);
    }

    public static Account social(UUID userId, AuthProvider provider, String providerSubject) {
        return new Account(userId, null, null, provider, providerSubject,
                AccountStatus.ACTIVE, 0, null);
    }

    public boolean isLockedAt(Instant now) {
        return status == AccountStatus.LOCKED && lockedUntil != null && now.isBefore(lockedUntil);
    }

    public boolean isDeleted() {
        return status == AccountStatus.DELETED || status == AccountStatus.DELETION_PENDING;
    }

    public Account unlockIfExpired(Instant now) {
        if (status == AccountStatus.LOCKED && (lockedUntil == null || !now.isBefore(lockedUntil))) {
            return new Account(userId, email, passwordHash, authProvider, providerSubject,
                    AccountStatus.ACTIVE, 0, null, createdAt, updatedAt);
        }
        return this;
    }

    public Account recordLoginFailure(Instant now, int maximumAttempts, Duration lockDuration) {
        int failures = loginFailCount + 1;
        if (failures >= maximumAttempts) {
            return new Account(userId, email, passwordHash, authProvider, providerSubject,
                    AccountStatus.LOCKED, failures, now.plus(lockDuration), createdAt, updatedAt);
        }
        return new Account(userId, email, passwordHash, authProvider, providerSubject,
                status, failures, lockedUntil, createdAt, updatedAt);
    }

    public Account recordLoginSuccess() {
        return new Account(userId, email, passwordHash, authProvider, providerSubject,
                AccountStatus.ACTIVE, 0, null, createdAt, updatedAt);
    }
}
