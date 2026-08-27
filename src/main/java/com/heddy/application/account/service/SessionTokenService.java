package com.heddy.application.account.service;

import com.heddy.domain.account.model.Account;
import com.heddy.domain.account.model.RefreshSession;
import com.heddy.domain.account.model.UserProfile;
import com.heddy.domain.account.port.in.AuthResult;
import com.heddy.domain.account.port.in.AuthTokens;
import com.heddy.domain.account.port.in.AuthUser;
import com.heddy.domain.account.port.out.AuthTokenPort;
import com.heddy.domain.account.port.out.RefreshSessionRepositoryPort;
import com.heddy.domain.account.port.out.SecureTokenGeneratorPort;
import com.heddy.domain.account.port.out.TokenHasherPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class SessionTokenService {

    private final AuthTokenPort authTokenPort;
    private final RefreshSessionRepositoryPort refreshSessionRepositoryPort;
    private final SecureTokenGeneratorPort secureTokenGeneratorPort;
    private final TokenHasherPort tokenHasherPort;
    private final long accessTokenSeconds;
    private final long refreshTokenSeconds;

    public SessionTokenService(
            AuthTokenPort authTokenPort,
            RefreshSessionRepositoryPort refreshSessionRepositoryPort,
            SecureTokenGeneratorPort secureTokenGeneratorPort,
            TokenHasherPort tokenHasherPort,
            @Value("${app.auth.access-token-seconds}") long accessTokenSeconds,
            @Value("${app.auth.refresh-token-seconds}") long refreshTokenSeconds
    ) {
        this.authTokenPort = authTokenPort;
        this.refreshSessionRepositoryPort = refreshSessionRepositoryPort;
        this.secureTokenGeneratorPort = secureTokenGeneratorPort;
        this.tokenHasherPort = tokenHasherPort;
        this.accessTokenSeconds = accessTokenSeconds;
        this.refreshTokenSeconds = refreshTokenSeconds;
    }

    public AuthResult issue(Account account, UserProfile profile) {
        return new AuthResult(
                new AuthUser(account.userId(), account.email(), profile.nickname(), account.status()),
                issueTokens(account.userId()));
    }

    public AuthTokens issueTokens(UUID userId) {
        Instant now = Instant.now();
        String rawRefreshToken = secureTokenGeneratorPort.generate();
        refreshSessionRepositoryPort.save(new RefreshSession(
                UUID.randomUUID(),
                userId,
                tokenHasherPort.hash(rawRefreshToken),
                now.plusSeconds(refreshTokenSeconds),
                null,
                null,
                now));
        return AuthTokens.bearer(
                authTokenPort.createAccessToken(userId), rawRefreshToken, accessTokenSeconds);
    }
}
