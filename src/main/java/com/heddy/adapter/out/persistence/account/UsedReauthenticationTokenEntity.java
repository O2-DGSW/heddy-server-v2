package com.heddy.adapter.out.persistence.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "used_reauthentication_tokens")
class UsedReauthenticationTokenEntity {

    @Id
    @Column(name = "token_id", nullable = false)
    private UUID tokenId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "used_at", nullable = false)
    private Instant usedAt;

    protected UsedReauthenticationTokenEntity() {
    }

    UsedReauthenticationTokenEntity(UUID tokenId, UUID userId, Instant usedAt) {
        this.tokenId = tokenId;
        this.userId = userId;
        this.usedAt = usedAt;
    }
}
