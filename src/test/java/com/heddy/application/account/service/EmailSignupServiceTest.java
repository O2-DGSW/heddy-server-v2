package com.heddy.application.account.service;

import com.heddy.domain.account.exception.AccountError;
import com.heddy.domain.account.exception.AccountException;
import com.heddy.domain.account.model.Account;
import com.heddy.domain.account.model.ConsentDecision;
import com.heddy.domain.account.model.ConsentType;
import com.heddy.domain.account.model.UserProfile;
import com.heddy.domain.account.port.in.EmailSignupCommand;
import com.heddy.domain.account.port.out.AccountRepositoryPort;
import com.heddy.domain.account.port.out.ConsentHistoryRepositoryPort;
import com.heddy.domain.account.port.out.PasswordEncoderPort;
import com.heddy.domain.account.port.out.UserProfileRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailSignupServiceTest {

    @Mock AccountRepositoryPort accountRepositoryPort;
    @Mock UserProfileRepositoryPort userProfileRepositoryPort;
    @Mock ConsentHistoryRepositoryPort consentHistoryRepositoryPort;
    @Mock PasswordEncoderPort passwordEncoderPort;
    @Mock SessionTokenService sessionTokenService;
    @Mock SignupPhoneVerificationService signupPhoneVerificationService;

    private EmailSignupService service;

    @BeforeEach
    void setUp() {
        service = new EmailSignupService(accountRepositoryPort, userProfileRepositoryPort,
                consentHistoryRepositoryPort, passwordEncoderPort, sessionTokenService,
                signupPhoneVerificationService);
    }

    @Test
    void createsUuidAccountProfileAndConsentHistory() {
        EmailSignupCommand command = command(true, true);
        given(passwordEncoderPort.encode("Password123")).willReturn("encoded");
        given(accountRepositoryPort.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        given(userProfileRepositoryPort.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        service.signup(command);

        ArgumentCaptor<Account> account = ArgumentCaptor.forClass(Account.class);
        verify(accountRepositoryPort).save(account.capture());
        assertThat(account.getValue().userId()).isNotNull();
        assertThat(account.getValue().email()).isEqualTo("user@example.com");
        assertThat(account.getValue().passwordHash()).isEqualTo("encoded");
        ArgumentCaptor<UserProfile> profile = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileRepositoryPort).save(profile.capture());
        assertThat(profile.getValue().userId()).isEqualTo(account.getValue().userId());
        verify(consentHistoryRepositoryPort).append(
                org.mockito.ArgumentMatchers.eq(account.getValue().userId()),
                org.mockito.ArgumentMatchers.eq(command.agreements()),
                any(), any());
    }

    @Test
    void rejectsDuplicatedEmail() {
        given(accountRepositoryPort.existsByEmail("user@example.com")).willReturn(true);

        assertError(() -> service.signup(command(true, true)), AccountError.EMAIL_ALREADY_EXISTS);
        verify(accountRepositoryPort, never()).save(any());
    }

    @Test
    void rejectsMissingRequiredConsent() {
        assertError(() -> service.signup(command(true, false)),
                AccountError.CONSENT_REQUIRED_NOT_GRANTED);
    }

    @Test
    void rejectsPasswordWithoutNumber() {
        EmailSignupCommand command = new EmailSignupCommand(
                "user@example.com", "onlyletters", "헤디", null, consents(true, true));
        assertError(() -> service.signup(command), AccountError.WEAK_PASSWORD);
    }

    private EmailSignupCommand command(boolean terms, boolean privacy) {
        return new EmailSignupCommand(
                "user@example.com", "Password123", "헤디", null, consents(terms, privacy));
    }

    private List<ConsentDecision> consents(boolean terms, boolean privacy) {
        return List.of(
                new ConsentDecision(ConsentType.TERMS_OF_SERVICE, terms, "2026-08-01"),
                new ConsentDecision(ConsentType.PRIVACY_POLICY, privacy, "2026-08-01"));
    }

    private void assertError(Runnable action, AccountError expected) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(AccountException.class,
                        exception -> assertThat(exception.error()).isEqualTo(expected));
    }
}
