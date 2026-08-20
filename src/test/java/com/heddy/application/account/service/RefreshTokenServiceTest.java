package com.heddy.application.account.service;

import com.heddy.domain.account.model.AccountRole;
import com.heddy.domain.account.model.AuthPrincipal;
import com.heddy.domain.account.exception.AccountError;
import com.heddy.domain.account.exception.AccountException;
import com.heddy.domain.account.port.in.AuthTokens;
import com.heddy.domain.account.port.out.AuthTokenPort;
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
class RefreshTokenServiceTest {

    @Mock AuthTokenPort authTokenPort;
    @Mock RefreshTokenStorePort refreshTokenStorePort;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(authTokenPort, refreshTokenStorePort);
    }

    @Test
    void refreshRotatesStoredToken() {
        AuthPrincipal principal = new AuthPrincipal(1L, AccountRole.USER);
        given(authTokenPort.parseRefreshToken("old")).willReturn(Optional.of(principal));
        given(authTokenPort.createAccessToken(1L, AccountRole.USER)).willReturn("access");
        given(authTokenPort.createRefreshToken(1L, AccountRole.USER)).willReturn("new");
        given(refreshTokenStorePort.rotate(1L, "old", "new")).willReturn(true);

        assertThat(refreshTokenService.refresh("old"))
                .isEqualTo(new AuthTokens("access", "new"));
    }

    @Test
    void reusedTokenInvalidatesSession() {
        AuthPrincipal principal = new AuthPrincipal(1L, AccountRole.USER);
        given(authTokenPort.parseRefreshToken("old")).willReturn(Optional.of(principal));
        given(authTokenPort.createAccessToken(1L, AccountRole.USER)).willReturn("access");
        given(authTokenPort.createRefreshToken(1L, AccountRole.USER)).willReturn("new");
        given(refreshTokenStorePort.rotate(1L, "old", "new")).willReturn(false);

        assertThatThrownBy(() -> refreshTokenService.refresh("old"))
                .isInstanceOfSatisfying(AccountException.class,
                        exception -> assertThat(exception.error())
                                .isEqualTo(AccountError.INVALID_REFRESH_TOKEN));
        verify(refreshTokenStorePort).delete(1L);
    }
}
