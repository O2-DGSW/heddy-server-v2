package com.heddy.application.account.service;

import com.heddy.domain.account.exception.AccountError;
import com.heddy.domain.account.exception.AccountException;
import com.heddy.domain.account.model.Account;
import com.heddy.domain.account.model.UserProfile;
import com.heddy.domain.account.port.in.EmailLoginCommand;
import com.heddy.domain.account.port.out.AccountRepositoryPort;
import com.heddy.domain.account.port.out.PasswordEncoderPort;
import com.heddy.domain.account.port.out.UserProfileRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailLoginServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock AccountRepositoryPort accountRepositoryPort;
    @Mock UserProfileRepositoryPort userProfileRepositoryPort;
    @Mock PasswordEncoderPort passwordEncoderPort;
    @Mock SessionTokenService sessionTokenService;

    private EmailLoginService service;

    @BeforeEach
    void setUp() {
        service = new EmailLoginService(accountRepositoryPort, userProfileRepositoryPort,
                passwordEncoderPort, sessionTokenService, 5, 1800);
    }

    @Test
    void successfulLoginClearsFailureStateAndIssuesSession() {
        Account account = Account.email(USER_ID, "user@example.com", "encoded");
        UserProfile profile = UserProfile.signup(USER_ID, "헤디");
        given(accountRepositoryPort.findByEmail("user@example.com")).willReturn(Optional.of(account));
        given(passwordEncoderPort.matches("Password123", "encoded")).willReturn(true);
        given(accountRepositoryPort.save(org.mockito.ArgumentMatchers.any()))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(userProfileRepositoryPort.findByUserId(USER_ID)).willReturn(Optional.of(profile));

        service.login(new EmailLoginCommand("user@example.com", "Password123"));

        verify(sessionTokenService).issue(account.recordLoginSuccess(), profile);
    }

    @Test
    void fifthFailurePersistsLockAndReturnsLockedError() {
        Account account = new Account(USER_ID, "user@example.com", "encoded",
                com.heddy.domain.account.model.AuthProvider.EMAIL, null,
                com.heddy.domain.account.model.AccountStatus.ACTIVE, 4, null);
        given(accountRepositoryPort.findByEmail("user@example.com")).willReturn(Optional.of(account));
        given(passwordEncoderPort.matches("wrong", "encoded")).willReturn(false);

        assertError(() -> service.login(new EmailLoginCommand("user@example.com", "wrong")),
                AccountError.ACCOUNT_LOCKED);

        ArgumentCaptor<Account> saved = ArgumentCaptor.forClass(Account.class);
        verify(accountRepositoryPort).save(saved.capture());
        assertThat(saved.getValue().loginFailCount()).isEqualTo(5);
        assertThat(saved.getValue().lockedUntil()).isNotNull();
    }

    @Test
    void firstFailureAfterLockExpiryStartsCounterAgain() {
        Account account = new Account(USER_ID, "user@example.com", "encoded",
                com.heddy.domain.account.model.AuthProvider.EMAIL, null,
                com.heddy.domain.account.model.AccountStatus.LOCKED, 5,
                Instant.now().minusSeconds(1));
        given(accountRepositoryPort.findByEmail("user@example.com")).willReturn(Optional.of(account));
        given(passwordEncoderPort.matches("wrong", "encoded")).willReturn(false);

        assertError(() -> service.login(new EmailLoginCommand("user@example.com", "wrong")),
                AccountError.INVALID_CREDENTIALS);

        ArgumentCaptor<Account> saved = ArgumentCaptor.forClass(Account.class);
        verify(accountRepositoryPort).save(saved.capture());
        assertThat(saved.getValue().loginFailCount()).isEqualTo(1);
        assertThat(saved.getValue().status())
                .isEqualTo(com.heddy.domain.account.model.AccountStatus.ACTIVE);
    }

    private void assertError(Runnable action, AccountError expected) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(AccountException.class,
                        exception -> assertThat(exception.error()).isEqualTo(expected));
    }
}
