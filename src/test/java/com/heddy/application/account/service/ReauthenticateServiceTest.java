package com.heddy.application.account.service;

import com.heddy.domain.account.exception.AccountError;
import com.heddy.domain.account.exception.AccountException;
import com.heddy.domain.account.model.Account;
import com.heddy.domain.account.model.AccountStatus;
import com.heddy.domain.account.model.AuthProvider;
import com.heddy.domain.account.port.in.ReauthenticateCommand;
import com.heddy.domain.account.port.out.AccountRepositoryPort;
import com.heddy.domain.account.port.out.AuthTokenPort;
import com.heddy.domain.account.port.out.PasswordEncoderPort;
import com.heddy.domain.account.port.out.SocialTokenVerifierPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReauthenticateServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock AccountRepositoryPort accountRepositoryPort;
    @Mock PasswordEncoderPort passwordEncoderPort;
    @Mock SocialTokenVerifierPort socialTokenVerifierPort;
    @Mock AuthTokenPort authTokenPort;

    private ReauthenticateService service;

    @BeforeEach
    void setUp() {
        service = new ReauthenticateService(accountRepositoryPort, passwordEncoderPort,
                socialTokenVerifierPort, authTokenPort, 300);
    }

    @Test
    void reauthenticatesEmailAccountWithPassword() {
        given(accountRepositoryPort.findById(USER_ID)).willReturn(Optional.of(emailAccount()));
        given(passwordEncoderPort.matches("Password123", "hash")).willReturn(true);
        given(authTokenPort.createReauthenticationToken(USER_ID)).willReturn("one-time-token");

        var result = service.reauthenticate(new ReauthenticateCommand(
                USER_ID, ReauthenticateCommand.Method.PASSWORD,
                "Password123", null, null));

        assertThat(result.reauthenticationToken()).isEqualTo("one-time-token");
        assertThat(result.expiresIn()).isEqualTo(300);
    }

    @Test
    void reauthenticatesSocialAccountWithFreshProviderToken() {
        Account account = new Account(USER_ID, null, null, AuthProvider.GOOGLE,
                "google-subject", AccountStatus.ACTIVE, 0, null);
        given(accountRepositoryPort.findById(USER_ID)).willReturn(Optional.of(account));
        given(socialTokenVerifierPort.verify(AuthProvider.GOOGLE, "fresh-provider-token"))
                .willReturn(Optional.of(() -> "google-subject"));
        given(authTokenPort.createReauthenticationToken(USER_ID)).willReturn("one-time-token");

        var result = service.reauthenticate(new ReauthenticateCommand(
                USER_ID, ReauthenticateCommand.Method.SOCIAL_TOKEN,
                null, AuthProvider.GOOGLE, "fresh-provider-token"));

        assertThat(result.reauthenticationToken()).isEqualTo("one-time-token");
        verify(socialTokenVerifierPort).verify(AuthProvider.GOOGLE, "fresh-provider-token");
    }

    @Test
    void rejectsDifferentProviderOrProviderSubject() {
        Account account = new Account(USER_ID, null, null, AuthProvider.GOOGLE,
                "google-subject", AccountStatus.ACTIVE, 0, null);
        given(accountRepositoryPort.findById(USER_ID)).willReturn(Optional.of(account));

        assertThatThrownBy(() -> service.reauthenticate(new ReauthenticateCommand(
                USER_ID, ReauthenticateCommand.Method.SOCIAL_TOKEN,
                null, AuthProvider.APPLE, "apple-token")))
                .isInstanceOf(AccountException.class)
                .hasFieldOrPropertyWithValue("error", AccountError.REAUTHENTICATION_REQUIRED);
        verify(socialTokenVerifierPort, never()).verify(AuthProvider.APPLE, "apple-token");

        given(socialTokenVerifierPort.verify(AuthProvider.GOOGLE, "google-token"))
                .willReturn(Optional.of(() -> "other-subject"));
        assertThatThrownBy(() -> service.reauthenticate(new ReauthenticateCommand(
                USER_ID, ReauthenticateCommand.Method.SOCIAL_TOKEN,
                null, AuthProvider.GOOGLE, "google-token")))
                .isInstanceOf(AccountException.class)
                .hasFieldOrPropertyWithValue("error", AccountError.REAUTHENTICATION_REQUIRED);
    }

    private Account emailAccount() {
        return new Account(USER_ID, "user@example.com", "hash", AuthProvider.EMAIL,
                null, AccountStatus.ACTIVE, 0, null);
    }
}
