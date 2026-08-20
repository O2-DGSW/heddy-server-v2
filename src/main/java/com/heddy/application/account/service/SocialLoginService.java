package com.heddy.application.account.service;

import com.heddy.domain.account.exception.AccountError;
import com.heddy.domain.account.exception.AccountException;
import com.heddy.domain.account.model.Account;
import com.heddy.domain.account.model.UserProfile;
import com.heddy.domain.account.port.in.AuthResult;
import com.heddy.domain.account.port.in.SocialLoginCommand;
import com.heddy.domain.account.port.in.SocialLoginUseCase;
import com.heddy.domain.account.port.out.AccountRepositoryPort;
import com.heddy.domain.account.port.out.SocialTokenVerifierPort;
import com.heddy.domain.account.port.out.UserProfileRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class SocialLoginService implements SocialLoginUseCase {

    private final SocialTokenVerifierPort socialTokenVerifierPort;
    private final AccountRepositoryPort accountRepositoryPort;
    private final UserProfileRepositoryPort userProfileRepositoryPort;
    private final SessionTokenService sessionTokenService;

    @Override
    @Transactional
    public AuthResult login(SocialLoginCommand command) {
        SocialTokenVerifierPort.VerifiedSocialIdentity identity = socialTokenVerifierPort
                .verify(command.provider(), command.providerToken())
                .orElseThrow(() -> new AccountException(AccountError.SOCIAL_TOKEN_INVALID));
        Account account = accountRepositoryPort.findByProvider(command.provider(), identity.subject())
                .orElseThrow(() -> new AccountException(AccountError.INVALID_CREDENTIALS));
        if (account.isDeleted()) {
            throw new AccountException(AccountError.ACCOUNT_DELETED);
        }
        Instant now = Instant.now();
        if (account.isLockedAt(now)) {
            throw new AccountException(AccountError.ACCOUNT_LOCKED);
        }
        Account active = account.unlockIfExpired(now);
        if (active != account) {
            active = accountRepositoryPort.save(active);
        }
        UserProfile profile = userProfileRepositoryPort.findByUserId(active.userId())
                .orElseThrow(() -> new AccountException(AccountError.ACCOUNT_NOT_FOUND));
        return sessionTokenService.issue(active, profile, command.device());
    }
}
