package com.heddy.application.account.service;

import com.heddy.domain.account.exception.AccountError;
import com.heddy.domain.account.exception.AccountException;
import com.heddy.domain.account.model.Account;
import com.heddy.domain.account.model.AccountDeletionRequest;
import com.heddy.domain.account.model.ReauthenticationPrincipal;
import com.heddy.domain.account.port.in.RequestAccountDeletionUseCase;
import com.heddy.domain.account.port.out.AccountDeletionRepositoryPort;
import com.heddy.domain.account.port.out.AccountRepositoryPort;
import com.heddy.domain.account.port.out.AuthTokenPort;
import com.heddy.domain.account.port.out.RefreshSessionRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AccountDeletionService implements RequestAccountDeletionUseCase {

    private final AccountRepositoryPort accountRepositoryPort;
    private final AccountDeletionRepositoryPort deletionRepositoryPort;
    private final RefreshSessionRepositoryPort refreshSessionRepositoryPort;
    private final AuthTokenPort authTokenPort;

    @Override
    @Transactional
    public AccountDeletionRequest request(Command command) {
        ReauthenticationPrincipal principal = authTokenPort
                .parseReauthenticationToken(command.reauthenticationToken())
                .filter(parsed -> parsed.userId().equals(command.userId()))
                .orElseThrow(() -> new AccountException(AccountError.REAUTHENTICATION_REQUIRED));
        Account account = accountRepositoryPort.findByIdForUpdate(command.userId())
                .orElseThrow(() -> new AccountException(AccountError.ACCOUNT_NOT_FOUND));

        if (account.isDeleted()) {
            return deletionRepositoryPort.findProcessingByUserId(command.userId())
                    .orElseThrow(() -> new AccountException(AccountError.ACCOUNT_DELETED));
        }
        Instant now = Instant.now();
        if (!deletionRepositoryPort.consumeReauthenticationToken(
                principal.tokenId(), command.userId(), now)) {
            throw new AccountException(AccountError.REAUTHENTICATION_TOKEN_REUSED);
        }

        AccountDeletionRequest request = deletionRepositoryPort.save(
                AccountDeletionRequest.processing(command.userId(), command.reason(), now));
        refreshSessionRepositoryPort.revokeAll(command.userId(), now);
        accountRepositoryPort.save(account.markDeletionPending());
        return request;
    }
}
