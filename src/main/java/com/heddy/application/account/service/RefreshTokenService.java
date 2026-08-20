package com.heddy.application.account.service;

import com.heddy.domain.account.model.AuthPrincipal;
import com.heddy.domain.account.exception.AccountError;
import com.heddy.domain.account.exception.AccountException;
import com.heddy.domain.account.port.in.AuthTokens;
import com.heddy.domain.account.port.in.RefreshTokenUseCase;
import com.heddy.domain.account.port.out.AuthTokenPort;
import com.heddy.domain.account.port.out.RefreshTokenStorePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService implements RefreshTokenUseCase {

    private final AuthTokenPort authTokenPort;
    private final RefreshTokenStorePort refreshTokenStorePort;

    @Override
    public AuthTokens refresh(String refreshToken) {
        AuthPrincipal principal = authTokenPort.parseRefreshToken(refreshToken)
                .orElseThrow(() -> new AccountException(AccountError.INVALID_REFRESH_TOKEN));

        String newAccessToken = authTokenPort.createAccessToken(principal.accountId(), principal.role());
        String newRefreshToken = authTokenPort.createRefreshToken(principal.accountId(), principal.role());
        if (!refreshTokenStorePort.rotate(principal.accountId(), refreshToken, newRefreshToken)) {
            refreshTokenStorePort.delete(principal.accountId());
            log.warn("Refresh token reuse detected for accountId={}", principal.accountId());
            throw new AccountException(AccountError.INVALID_REFRESH_TOKEN);
        }
        return new AuthTokens(newAccessToken, newRefreshToken);
    }
}
