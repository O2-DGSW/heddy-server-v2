package com.heddy.domain.account.model;

import java.time.Instant;
import java.util.UUID;

public record AccountDeletionRequest(
        UUID deletionRequestId,
        UUID userId,
        AccountDeletionStatus status,
        String reason,
        Instant requestedAt,
        Instant completedAt
) {
    public static AccountDeletionRequest processing(UUID userId, String reason, Instant now) {
        String normalizedReason = reason == null || reason.isBlank() ? null : reason.strip();
        return new AccountDeletionRequest(
                UUID.randomUUID(), userId, AccountDeletionStatus.PROCESSING,
                normalizedReason, now, null);
    }

    public AccountDeletionRequest complete(Instant now) {
        return new AccountDeletionRequest(
                deletionRequestId, userId, AccountDeletionStatus.COMPLETED,
                reason, requestedAt, now);
    }

    public AccountDeletionRequest fail(Instant now) {
        return new AccountDeletionRequest(
                deletionRequestId, userId, AccountDeletionStatus.FAILED,
                reason, requestedAt, now);
    }
}
