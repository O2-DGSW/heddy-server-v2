package com.heddy.application.account.service;

import com.heddy.domain.account.exception.AccountError;
import com.heddy.domain.account.exception.AccountException;
import com.heddy.domain.account.model.Account;
import com.heddy.domain.account.model.ConsentSource;
import com.heddy.domain.account.model.UserProfile;
import com.heddy.domain.account.port.in.AuthResult;
import com.heddy.domain.account.port.in.SocialSignupCommand;
import com.heddy.domain.account.port.in.SocialSignupUseCase;
import com.heddy.domain.account.port.out.AccountRepositoryPort;
import com.heddy.domain.account.port.out.ConsentHistoryRepositoryPort;
import com.heddy.domain.account.port.out.SocialTokenVerifierPort;
import com.heddy.domain.account.port.out.UserProfileRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SocialSignupService implements SocialSignupUseCase {

    private final SocialTokenVerifierPort socialTokenVerifierPort;
    private final AccountRepositoryPort accountRepositoryPort;
    private final UserProfileRepositoryPort userProfileRepositoryPort;
    private final ConsentHistoryRepositoryPort consentHistoryRepositoryPort;
    private final SessionTokenService sessionTokenService;
    private final SignupPhoneVerificationService signupPhoneVerificationService;

    @Override
    @Transactional
    public AuthResult signup(SocialSignupCommand command) {
        ConsentValidator.requireSignupConsents(command.agreements());
        signupPhoneVerificationService.validate(command.phone());
        SocialTokenVerifierPort.VerifiedSocialIdentity identity = socialTokenVerifierPort
                .verify(command.provider(), command.providerToken())
                .orElseThrow(() -> new AccountException(AccountError.SOCIAL_TOKEN_INVALID));
        if (accountRepositoryPort.findByProvider(command.provider(), identity.subject()).isPresent()) {
            throw new AccountException(AccountError.SOCIAL_ACCOUNT_ALREADY_LINKED);
        }

        UUID userId = UUID.randomUUID();
        Account account = accountRepositoryPort.save(
                Account.social(userId, command.provider(), identity.subject()));
        UserProfile profile = userProfileRepositoryPort.save(
                UserProfile.signup(userId, command.nickname(), command.phone()));
        consentHistoryRepositoryPort.append(
                userId, command.agreements(), ConsentSource.SIGNUP, Instant.now());
        AuthResult result = sessionTokenService.issue(account, profile, null);
        signupPhoneVerificationService.consume(command.phone());
        return result;
    }
}
