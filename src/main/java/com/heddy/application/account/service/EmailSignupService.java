package com.heddy.application.account.service;

import com.heddy.domain.account.exception.AccountError;
import com.heddy.domain.account.exception.AccountException;
import com.heddy.domain.account.model.Account;
import com.heddy.domain.account.model.ConsentSource;
import com.heddy.domain.account.model.UserProfile;
import com.heddy.domain.account.port.in.AuthResult;
import com.heddy.domain.account.port.in.EmailSignupCommand;
import com.heddy.domain.account.port.in.EmailSignupUseCase;
import com.heddy.domain.account.port.out.AccountRepositoryPort;
import com.heddy.domain.account.port.out.ConsentHistoryRepositoryPort;
import com.heddy.domain.account.port.out.PasswordEncoderPort;
import com.heddy.domain.account.port.out.UserProfileRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailSignupService implements EmailSignupUseCase {

    private final AccountRepositoryPort accountRepositoryPort;
    private final UserProfileRepositoryPort userProfileRepositoryPort;
    private final ConsentHistoryRepositoryPort consentHistoryRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final SessionTokenService sessionTokenService;
    private final SignupPhoneVerificationService signupPhoneVerificationService;

    @Override
    @Transactional
    public AuthResult signup(EmailSignupCommand command) {
        PasswordPolicy.validate(command.password());
        ConsentValidator.requireSignupConsents(command.agreements());
        signupPhoneVerificationService.validate(command.phone());
        if (accountRepositoryPort.existsByEmail(command.email())) {
            throw new AccountException(AccountError.EMAIL_ALREADY_EXISTS);
        }

        UUID userId = UUID.randomUUID();
        Account account = accountRepositoryPort.save(Account.email(
                userId, command.email(), passwordEncoderPort.encode(command.password())));
        UserProfile profile = userProfileRepositoryPort.save(
                UserProfile.signup(userId, command.nickname(), command.phone()));
        consentHistoryRepositoryPort.append(
                userId, command.agreements(), ConsentSource.SIGNUP, Instant.now());
        AuthResult result = sessionTokenService.issue(account, profile);
        signupPhoneVerificationService.consume(command.phone());
        return result;
    }
}
