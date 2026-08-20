package com.heddy.application.account.service;

import com.heddy.domain.account.model.Account;
import com.heddy.domain.account.model.AccountStatus;
import com.heddy.domain.account.exception.AccountError;
import com.heddy.domain.account.exception.AccountException;
import com.heddy.domain.account.port.in.AuthTokens;
import com.heddy.domain.account.port.in.LoginCommand;
import com.heddy.domain.account.port.in.LoginUseCase;
import com.heddy.domain.account.port.out.AccountRepositoryPort;
import com.heddy.domain.account.port.out.AuthTokenPort;
import com.heddy.domain.account.port.out.PasswordEncoderPort;
import com.heddy.domain.account.port.out.RefreshTokenStorePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginService implements LoginUseCase {

    private final AccountRepositoryPort accountRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final AuthTokenPort authTokenPort;
    private final RefreshTokenStorePort refreshTokenStorePort;

    @Override
    public AuthTokens login(LoginCommand command) {
        Account account = accountRepositoryPort.findByLoginId(command.loginId())
                .orElseThrow(() -> new AccountException(AccountError.LOGIN_FAILED));
        if (account.encodedPassword() == null
                || !passwordEncoderPort.matches(command.password(), account.encodedPassword())) {
            throw new AccountException(AccountError.LOGIN_FAILED);
        }
        validateStatus(account);

        String accessToken = authTokenPort.createAccessToken(account.id(), account.role());
        String refreshToken = authTokenPort.createRefreshToken(account.id(), account.role());
        refreshTokenStorePort.save(account.id(), refreshToken);
        return new AuthTokens(accessToken, refreshToken);
    }

    private void validateStatus(Account account) {
        if (account.status() == AccountStatus.INACTIVE) {
            throw new AccountException(AccountError.ACCOUNT_INACTIVE);
        }
        if (account.status() == AccountStatus.SUSPENDED) {
            throw new AccountException(AccountError.ACCOUNT_SUSPENDED);
        }
    }
}
