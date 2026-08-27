package com.heddy.adapter.out.persistence.account;

import com.heddy.domain.account.model.RefreshSession;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
class RefreshTokenEntity {

    @Id
    @Column(name = "refresh_token_id", nullable = false)
    private UUID refreshTokenId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "rotated_to")
    private UUID rotatedTo;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RefreshTokenEntity() {
    }

    RefreshTokenEntity(RefreshSession session) {
        refreshTokenId = session.refreshTokenId();
        userId = session.userId();
        tokenHash = session.tokenHash();
        expiresAt = session.expiresAt();
        rotatedTo = session.rotatedTo();
        revokedAt = session.revokedAt();
        createdAt = session.createdAt();
    }

    RefreshSession toDomain() {
        return new RefreshSession(refreshTokenId, userId, tokenHash,
                expiresAt, rotatedTo, revokedAt, createdAt);
    }

    void rotate(UUID nextId, Instant now) {
        rotatedTo = nextId;
        revokedAt = now;
    }

    void revoke(Instant now) {
        if (revokedAt == null) {
            revokedAt = now;
        }
    }
}
