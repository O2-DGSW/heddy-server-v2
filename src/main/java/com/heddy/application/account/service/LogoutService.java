package com.heddy.application.account.service;

import com.heddy.domain.account.port.in.LogoutUseCase;
import com.heddy.domain.account.port.out.RefreshSessionRepositoryPort;
import com.heddy.domain.account.port.out.TokenHasherPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LogoutService implements LogoutUseCase {

    private final RefreshSessionRepositoryPort refreshSessionRepositoryPort;
    private final TokenHasherPort tokenHasherPort;

    @Override
    @Transactional
    public void logout(UUID userId, String refreshToken) {
        refreshSessionRepositoryPort.findByTokenHashForUpdate(tokenHasherPort.hash(refreshToken))
                .filter(session -> session.userId().equals(userId))
                .ifPresent(session -> refreshSessionRepositoryPort.revoke(
                        session.refreshTokenId(), Instant.now()));
    }
}
