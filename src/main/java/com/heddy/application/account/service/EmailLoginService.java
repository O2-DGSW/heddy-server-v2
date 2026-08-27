package com.heddy.application.account.service;

import com.heddy.domain.account.exception.AccountError;
import com.heddy.domain.account.exception.AccountException;
import com.heddy.domain.account.model.Account;
import com.heddy.domain.account.model.UserProfile;
import com.heddy.domain.account.port.in.AuthResult;
import com.heddy.domain.account.port.in.EmailLoginCommand;
import com.heddy.domain.account.port.in.EmailLoginUseCase;
import com.heddy.domain.account.port.out.AccountRepositoryPort;
import com.heddy.domain.account.port.out.PasswordEncoderPort;
import com.heddy.domain.account.port.out.UserProfileRepositoryPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
public class EmailLoginService implements EmailLoginUseCase {

    private final AccountRepositoryPort accountRepositoryPort;
    private final UserProfileRepositoryPort userProfileRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final SessionTokenService sessionTokenService;
    private final int maximumAttempts;
    private final Duration lockDuration;

    public EmailLoginService(
            AccountRepositoryPort accountRepositoryPort,
            UserProfileRepositoryPort userProfileRepositoryPort,
            PasswordEncoderPort passwordEncoderPort,
            SessionTokenService sessionTokenService,
            @Value("${app.auth.login.maximum-attempts:5}") int maximumAttempts,
            @Value("${app.auth.login.lock-seconds:900}") long lockSeconds
    ) {
        this.accountRepositoryPort = accountRepositoryPort;
        this.userProfileRepositoryPort = userProfileRepositoryPort;
        this.passwordEncoderPort = passwordEncoderPort;
        this.sessionTokenService = sessionTokenService;
        this.maximumAttempts = maximumAttempts;
        this.lockDuration = Duration.ofSeconds(lockSeconds);
    }

    @Override
    @Transactional(noRollbackFor = AccountException.class)
    public AuthResult login(EmailLoginCommand command) {
        Account account = accountRepositoryPort.findByEmail(command.email())
                .orElseThrow(() -> new AccountException(AccountError.INVALID_CREDENTIALS));
        Instant now = Instant.now();
        if (account.isDeleted()) {
            throw new AccountException(AccountError.ACCOUNT_DELETED);
        }
        if (account.isLockedAt(now)) {
            throw new AccountException(AccountError.ACCOUNT_LOCKED);
        }
        account = account.unlockIfExpired(now);
        if (account.passwordHash() == null
                || !passwordEncoderPort.matches(command.password(), account.passwordHash())) {
            Account failed = account.recordLoginFailure(now, maximumAttempts, lockDuration);
            accountRepositoryPort.save(failed);
            if (failed.isLockedAt(now)) {
                throw new AccountException(AccountError.ACCOUNT_LOCKED);
            }
            throw new AccountException(AccountError.INVALID_CREDENTIALS);
        }

        Account active = accountRepositoryPort.save(account.recordLoginSuccess());
        UserProfile profile = userProfileRepositoryPort.findByUserId(active.userId())
                .orElseThrow(() -> new AccountException(AccountError.ACCOUNT_NOT_FOUND));
        return sessionTokenService.issue(active, profile);
    }
}
