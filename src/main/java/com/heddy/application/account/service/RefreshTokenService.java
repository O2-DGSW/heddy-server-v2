package com.heddy.application.account.service;

import com.heddy.domain.account.exception.AccountError;
import com.heddy.domain.account.exception.AccountException;
import com.heddy.domain.account.model.RefreshSession;
import com.heddy.domain.account.port.in.AuthTokens;
import com.heddy.domain.account.port.in.RefreshTokenUseCase;
import com.heddy.domain.account.port.out.AuthTokenPort;
import com.heddy.domain.account.port.out.RefreshSessionRepositoryPort;
import com.heddy.domain.account.port.out.SecureTokenGeneratorPort;
import com.heddy.domain.account.port.out.TokenHasherPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class RefreshTokenService implements RefreshTokenUseCase {

    private final RefreshSessionRepositoryPort refreshSessionRepositoryPort;
    private final SecureTokenGeneratorPort secureTokenGeneratorPort;
    private final TokenHasherPort tokenHasherPort;
    private final AuthTokenPort authTokenPort;
    private final long accessTokenSeconds;
    private final long refreshTokenSeconds;

    public RefreshTokenService(
            RefreshSessionRepositoryPort refreshSessionRepositoryPort,
            SecureTokenGeneratorPort secureTokenGeneratorPort,
            TokenHasherPort tokenHasherPort,
            AuthTokenPort authTokenPort,
            @Value("${app.auth.access-token-seconds}") long accessTokenSeconds,
            @Value("${app.auth.refresh-token-seconds}") long refreshTokenSeconds
    ) {
        this.refreshSessionRepositoryPort = refreshSessionRepositoryPort;
        this.secureTokenGeneratorPort = secureTokenGeneratorPort;
        this.tokenHasherPort = tokenHasherPort;
        this.authTokenPort = authTokenPort;
        this.accessTokenSeconds = accessTokenSeconds;
        this.refreshTokenSeconds = refreshTokenSeconds;
    }

    @Override
    @Transactional(noRollbackFor = AccountException.class)
    public AuthTokens refresh(String refreshToken) {
        Instant now = Instant.now();
        RefreshSession current = refreshSessionRepositoryPort
                .findByTokenHashForUpdate(tokenHasherPort.hash(refreshToken))
                .orElseThrow(() -> new AccountException(AccountError.REFRESH_TOKEN_INVALID));
        if (current.wasRotated()) {
            refreshSessionRepositoryPort.revokeAll(current.userId(), now);
            throw new AccountException(AccountError.REFRESH_TOKEN_REUSED);
        }
        if (current.isRevoked() || current.isExpiredAt(now)) {
            throw new AccountException(AccountError.REFRESH_TOKEN_INVALID);
        }

        String nextRawToken = secureTokenGeneratorPort.generate();
        RefreshSession next = new RefreshSession(
                UUID.randomUUID(), current.userId(), tokenHasherPort.hash(nextRawToken), current.device(),
                now.plusSeconds(refreshTokenSeconds), null, null, now);
        refreshSessionRepositoryPort.save(next);
        refreshSessionRepositoryPort.rotate(current.refreshTokenId(), next.refreshTokenId(), now);
        return AuthTokens.bearer(
                authTokenPort.createAccessToken(current.userId()), nextRawToken, accessTokenSeconds);
    }
}
