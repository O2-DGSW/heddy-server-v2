package com.heddy.domain.account.port.out;

import com.heddy.domain.account.model.RefreshSession;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshSessionRepositoryPort {
    RefreshSession save(RefreshSession session);
    Optional<RefreshSession> findByTokenHashForUpdate(String tokenHash);
    void rotate(UUID currentId, UUID nextId, Instant revokedAt);
    void revoke(UUID refreshTokenId, Instant revokedAt);
    void revokeAll(UUID userId, Instant revokedAt);
}
