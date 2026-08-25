package com.heddy.application.account.service;

import com.heddy.domain.account.exception.AccountError;
import com.heddy.domain.account.exception.AccountException;
import com.heddy.domain.account.model.Account;
import com.heddy.domain.account.model.AccountDeletionRequest;
import com.heddy.domain.account.model.AccountStatus;
import com.heddy.domain.account.model.AuthProvider;
import com.heddy.domain.account.model.ReauthenticationPrincipal;
import com.heddy.domain.account.port.in.RequestAccountDeletionUseCase;
import com.heddy.domain.account.port.out.AccountDeletionRepositoryPort;
import com.heddy.domain.account.port.out.AccountRepositoryPort;
import com.heddy.domain.account.port.out.AuthTokenPort;
import com.heddy.domain.account.port.out.RefreshSessionRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountDeletionServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID TOKEN_ID = UUID.randomUUID();

    @Mock AccountRepositoryPort accountRepositoryPort;
    @Mock AccountDeletionRepositoryPort deletionRepositoryPort;
    @Mock RefreshSessionRepositoryPort refreshSessionRepositoryPort;
    @Mock AuthTokenPort authTokenPort;

    private AccountDeletionService service;

    @BeforeEach
    void setUp() {
        service = new AccountDeletionService(accountRepositoryPort, deletionRepositoryPort,
                refreshSessionRepositoryPort, authTokenPort);
    }

    @Test
    void consumesReauthenticationTokenRevokesSessionsAndBlocksAccount() {
        Account account = activeAccount();
        given(authTokenPort.parseReauthenticationToken("token"))
                .willReturn(Optional.of(new ReauthenticationPrincipal(USER_ID, TOKEN_ID)));
        given(accountRepositoryPort.findByIdForUpdate(USER_ID)).willReturn(Optional.of(account));
        given(deletionRepositoryPort.consumeReauthenticationToken(any(), any(), any()))
                .willReturn(true);
        given(deletionRepositoryPort.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        AccountDeletionRequest result = service.request(
                new RequestAccountDeletionUseCase.Command(USER_ID, "token", " 사유 "));

        assertThat(result.status().name()).isEqualTo("PROCESSING");
        assertThat(result.reason()).isEqualTo("사유");
        verify(refreshSessionRepositoryPort).revokeAll(any(), any());
        verify(accountRepositoryPort).save(org.mockito.ArgumentMatchers.argThat(saved ->
                saved.status() == AccountStatus.DELETION_PENDING));
    }

    @Test
    void rejectsWrongOrReusedReauthenticationToken() {
        given(authTokenPort.parseReauthenticationToken("wrong"))
                .willReturn(Optional.of(new ReauthenticationPrincipal(UUID.randomUUID(), TOKEN_ID)));
        assertThatThrownBy(() -> service.request(
                new RequestAccountDeletionUseCase.Command(USER_ID, "wrong", null)))
                .isInstanceOf(AccountException.class)
                .hasFieldOrPropertyWithValue("error", AccountError.REAUTHENTICATION_REQUIRED);

        given(authTokenPort.parseReauthenticationToken("used"))
                .willReturn(Optional.of(new ReauthenticationPrincipal(USER_ID, TOKEN_ID)));
        given(accountRepositoryPort.findByIdForUpdate(USER_ID)).willReturn(Optional.of(activeAccount()));
        given(deletionRepositoryPort.consumeReauthenticationToken(any(), any(), any()))
                .willReturn(false);
        assertThatThrownBy(() -> service.request(
                new RequestAccountDeletionUseCase.Command(USER_ID, "used", null)))
                .isInstanceOf(AccountException.class)
                .hasFieldOrPropertyWithValue("error", AccountError.REAUTHENTICATION_TOKEN_REUSED);
    }

    @Test
    void returnsExistingProcessingRequestForDuplicateDeletion() {
        Account pending = activeAccount().markDeletionPending();
        AccountDeletionRequest existing = AccountDeletionRequest.processing(USER_ID, null, Instant.now());
        given(authTokenPort.parseReauthenticationToken("token"))
                .willReturn(Optional.of(new ReauthenticationPrincipal(USER_ID, TOKEN_ID)));
        given(accountRepositoryPort.findByIdForUpdate(USER_ID)).willReturn(Optional.of(pending));
        given(deletionRepositoryPort.findProcessingByUserId(USER_ID)).willReturn(Optional.of(existing));

        assertThat(service.request(new RequestAccountDeletionUseCase.Command(
                USER_ID, "token", null))).isEqualTo(existing);
    }

    private Account activeAccount() {
        return new Account(USER_ID, "user@example.com", "hash", AuthProvider.EMAIL,
                null, AccountStatus.ACTIVE, 0, null);
    }
}
