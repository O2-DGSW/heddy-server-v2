package com.heddy.application.account.service;

import com.heddy.domain.account.model.Account;
import com.heddy.domain.account.model.AccountRole;
import com.heddy.domain.account.model.AccountStatus;
import com.heddy.domain.account.exception.AccountError;
import com.heddy.domain.account.exception.AccountException;
import com.heddy.domain.account.port.in.AuthTokens;
import com.heddy.domain.account.port.in.LoginCommand;
import com.heddy.domain.account.port.out.AccountRepositoryPort;
import com.heddy.domain.account.port.out.AuthTokenPort;
import com.heddy.domain.account.port.out.PasswordEncoderPort;
import com.heddy.domain.account.port.out.RefreshTokenStorePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock AccountRepositoryPort accountRepositoryPort;
    @Mock PasswordEncoderPort passwordEncoderPort;
    @Mock AuthTokenPort authTokenPort;
    @Mock RefreshTokenStorePort refreshTokenStorePort;

    private LoginService loginService;

    @BeforeEach
    void setUp() {
        loginService = new LoginService(
                accountRepositoryPort, passwordEncoderPort, authTokenPort, refreshTokenStorePort);
    }

    @Test
    void loginIssuesAndStoresTokens() {
        Account account = account(AccountStatus.ACTIVE);
        given(accountRepositoryPort.findByLoginId("mola")).willReturn(Optional.of(account));
        given(passwordEncoderPort.matches("password!1", "encoded")).willReturn(true);
        given(authTokenPort.createAccessToken(1L, AccountRole.USER)).willReturn("access");
        given(authTokenPort.createRefreshToken(1L, AccountRole.USER)).willReturn("refresh");

        AuthTokens tokens = loginService.login(new LoginCommand("mola", "password!1"));

        assertThat(tokens).isEqualTo(new AuthTokens("access", "refresh"));
        verify(refreshTokenStorePort).save(1L, "refresh");
    }

    @Test
    void wrongPasswordUsesSameLoginFailureAsUnknownAccount() {
        given(accountRepositoryPort.findByLoginId("mola"))
                .willReturn(Optional.of(account(AccountStatus.ACTIVE)));
        given(passwordEncoderPort.matches("wrong", "encoded")).willReturn(false);

        assertError(new LoginCommand("mola", "wrong"), AccountError.LOGIN_FAILED);
    }

    @Test
    void suspendedAccountCannotLogin() {
        given(accountRepositoryPort.findByLoginId("mola"))
                .willReturn(Optional.of(account(AccountStatus.SUSPENDED)));
        given(passwordEncoderPort.matches("password!1", "encoded")).willReturn(true);

        assertError(new LoginCommand("mola", "password!1"), AccountError.ACCOUNT_SUSPENDED);
    }

    private void assertError(LoginCommand command, AccountError error) {
        assertThatThrownBy(() -> loginService.login(command))
                .isInstanceOfSatisfying(AccountException.class,
                        exception -> assertThat(exception.error()).isEqualTo(error));
    }

    private Account account(AccountStatus status) {
        return new Account(1L, "mola", "encoded", "몰라", "01012345678",
                AccountRole.USER, status, true);
    }
}
