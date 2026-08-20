package com.heddy.adapter.out.persistence.account;

import com.heddy.domain.account.model.DeviceInfo;
import com.heddy.domain.account.model.RefreshSession;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    @Column(name = "device_id", length = 100)
    private String deviceId;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private DeviceInfo.Platform platform;

    @Column(name = "app_version", length = 20)
    private String appVersion;

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
        if (session.device() != null) {
            deviceId = session.device().deviceId();
            platform = session.device().platform();
            appVersion = session.device().appVersion();
        }
        expiresAt = session.expiresAt();
        rotatedTo = session.rotatedTo();
        revokedAt = session.revokedAt();
        createdAt = session.createdAt();
    }

    RefreshSession toDomain() {
        DeviceInfo device = deviceId == null && platform == null && appVersion == null
                ? null : new DeviceInfo(deviceId, platform, appVersion);
        return new RefreshSession(refreshTokenId, userId, tokenHash, device,
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
