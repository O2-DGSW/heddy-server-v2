package com.heddy.application.account.service;

import com.heddy.domain.account.exception.AccountError;
import com.heddy.domain.account.exception.AccountException;
import com.heddy.domain.account.model.Account;
import com.heddy.domain.account.model.AuthProvider;
import com.heddy.domain.account.model.AccountStatus;
import com.heddy.domain.account.model.RefreshSession;
import com.heddy.domain.account.port.in.AuthTokens;
import com.heddy.domain.account.port.out.AuthTokenPort;
import com.heddy.domain.account.port.out.AccountRepositoryPort;
import com.heddy.domain.account.port.out.RefreshSessionRepositoryPort;
import com.heddy.domain.account.port.out.SecureTokenGeneratorPort;
import com.heddy.domain.account.port.out.TokenHasherPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CURRENT_ID = UUID.randomUUID();

    @Mock RefreshSessionRepositoryPort refreshSessionRepositoryPort;
    @Mock SecureTokenGeneratorPort secureTokenGeneratorPort;
    @Mock TokenHasherPort tokenHasherPort;
    @Mock AuthTokenPort authTokenPort;
    @Mock AccountRepositoryPort accountRepositoryPort;

    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        service = new RefreshTokenService(refreshSessionRepositoryPort, secureTokenGeneratorPort,
                tokenHasherPort, authTokenPort, accountRepositoryPort, 900, 2592000);
    }

    @Test
    void rotatesOpaqueRefreshTokenAndStoresOnlyHash() {
        RefreshSession current = new RefreshSession(CURRENT_ID, USER_ID, "old-hash",
                Instant.now().plusSeconds(600), null, null, Instant.now());
        given(tokenHasherPort.hash("raw-old")).willReturn("old-hash");
        given(refreshSessionRepositoryPort.findByTokenHashForUpdate("old-hash"))
                .willReturn(Optional.of(current));
        given(secureTokenGeneratorPort.generate()).willReturn("raw-next");
        given(tokenHasherPort.hash("raw-next")).willReturn("next-hash");
        given(authTokenPort.createAccessToken(USER_ID)).willReturn("access");
        given(accountRepositoryPort.findById(USER_ID)).willReturn(Optional.of(activeAccount()));

        AuthTokens result = service.refresh("raw-old");

        assertThat(result).isEqualTo(new AuthTokens("access", "raw-next", "Bearer", 900));
        ArgumentCaptor<RefreshSession> next = ArgumentCaptor.forClass(RefreshSession.class);
        verify(refreshSessionRepositoryPort).save(next.capture());
        assertThat(next.getValue().tokenHash()).isEqualTo("next-hash");
        assertThat(next.getValue().tokenHash()).doesNotContain("raw-next");
        verify(refreshSessionRepositoryPort).rotate(
                org.mockito.ArgumentMatchers.eq(CURRENT_ID),
                org.mockito.ArgumentMatchers.eq(next.getValue().refreshTokenId()),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsRefreshForDeletionPendingAccountAndRevokesSessions() {
        RefreshSession current = new RefreshSession(CURRENT_ID, USER_ID, "old-hash",
                Instant.now().plusSeconds(600), null, null, Instant.now());
        given(tokenHasherPort.hash("raw-old")).willReturn("old-hash");
        given(refreshSessionRepositoryPort.findByTokenHashForUpdate("old-hash"))
                .willReturn(Optional.of(current));
        Account pending = new Account(USER_ID, "user@example.com", "hash", AuthProvider.EMAIL,
                null, AccountStatus.DELETION_PENDING, 0, null);
        given(accountRepositoryPort.findById(USER_ID)).willReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.refresh("raw-old"))
                .isInstanceOfSatisfying(AccountException.class,
                        exception -> assertThat(exception.error()).isEqualTo(AccountError.ACCOUNT_DELETED));
        verify(refreshSessionRepositoryPort).revokeAll(
                org.mockito.ArgumentMatchers.eq(USER_ID), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void reusedRotatedTokenRevokesEveryActiveSession() {
        RefreshSession reused = new RefreshSession(CURRENT_ID, USER_ID, "old-hash",
                Instant.now().plusSeconds(600), UUID.randomUUID(), Instant.now(), Instant.now());
        given(tokenHasherPort.hash("raw-old")).willReturn("old-hash");
        given(refreshSessionRepositoryPort.findByTokenHashForUpdate("old-hash"))
                .willReturn(Optional.of(reused));

        assertThatThrownBy(() -> service.refresh("raw-old"))
                .isInstanceOfSatisfying(AccountException.class,
                        exception -> assertThat(exception.error()).isEqualTo(AccountError.REFRESH_TOKEN_REUSED));
        verify(refreshSessionRepositoryPort).revokeAll(
                org.mockito.ArgumentMatchers.eq(USER_ID), org.mockito.ArgumentMatchers.any());
    }

    private Account activeAccount() {
        return new Account(USER_ID, "user@example.com", "hash", AuthProvider.EMAIL,
                null, AccountStatus.ACTIVE, 0, null);
    }
}
