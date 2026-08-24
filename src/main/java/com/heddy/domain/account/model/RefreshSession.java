package com.heddy.domain.account.model;

import java.time.Instant;
import java.util.UUID;

public record RefreshSession(
        UUID refreshTokenId,
        UUID userId,
        String tokenHash,
        DeviceInfo device,
        Instant expiresAt,
        UUID rotatedTo,
        Instant revokedAt,
        Instant createdAt
) {
    public boolean isExpiredAt(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public boolean wasRotated() {
        return rotatedTo != null;
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }
}
