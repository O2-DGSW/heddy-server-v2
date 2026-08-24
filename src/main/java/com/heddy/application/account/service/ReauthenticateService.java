package com.heddy.application.account.service;

import com.heddy.domain.account.exception.AccountError;
import com.heddy.domain.account.exception.AccountException;
import com.heddy.domain.account.model.Account;
import com.heddy.domain.account.port.in.ReauthenticateCommand;
import com.heddy.domain.account.port.in.ReauthenticateResult;
import com.heddy.domain.account.port.in.ReauthenticateUseCase;
import com.heddy.domain.account.port.out.AccountRepositoryPort;
import com.heddy.domain.account.port.out.AuthTokenPort;
import com.heddy.domain.account.port.out.PasswordEncoderPort;
import com.heddy.domain.account.port.out.SocialTokenVerifierPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class ReauthenticateService implements ReauthenticateUseCase {

    private final AccountRepositoryPort accountRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final SocialTokenVerifierPort socialTokenVerifierPort;
    private final AuthTokenPort authTokenPort;
    private final long expiresIn;

    public ReauthenticateService(
            AccountRepositoryPort accountRepositoryPort,
            PasswordEncoderPort passwordEncoderPort,
            SocialTokenVerifierPort socialTokenVerifierPort,
            AuthTokenPort authTokenPort,
            @Value("${app.auth.reauthentication-token-seconds:300}") long expiresIn
    ) {
        this.accountRepositoryPort = accountRepositoryPort;
        this.passwordEncoderPort = passwordEncoderPort;
        this.socialTokenVerifierPort = socialTokenVerifierPort;
        this.authTokenPort = authTokenPort;
        this.expiresIn = expiresIn;
    }

    @Override
    public ReauthenticateResult reauthenticate(ReauthenticateCommand command) {
        Account account = accountRepositoryPort.findById(command.userId())
                .orElseThrow(() -> new AccountException(AccountError.REAUTHENTICATION_REQUIRED));
        if (account.isDeleted()) {
            throw new AccountException(AccountError.ACCOUNT_DELETED);
        }
        if (account.isLockedAt(Instant.now())) {
            throw new AccountException(AccountError.ACCOUNT_LOCKED);
        }
        boolean verified = switch (command.method()) {
            case PASSWORD -> command.password() != null && account.passwordHash() != null
                    && passwordEncoderPort.matches(command.password(), account.passwordHash());
            case SOCIAL_TOKEN -> command.provider() != null && command.providerToken() != null
                    && command.provider() == account.authProvider()
                    && socialTokenVerifierPort.verify(command.provider(), command.providerToken())
                    .map(identity -> identity.subject().equals(account.providerSubject()))
                    .orElse(false);
        };
        if (!verified) {
            throw new AccountException(AccountError.REAUTHENTICATION_REQUIRED);
        }
        return new ReauthenticateResult(
                authTokenPort.createReauthenticationToken(account.userId()), expiresIn);
    }
}
