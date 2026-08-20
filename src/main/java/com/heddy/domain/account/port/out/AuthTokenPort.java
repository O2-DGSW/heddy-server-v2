package com.heddy.domain.account.port.out;

import com.heddy.domain.account.model.AccountRole;
import com.heddy.domain.account.model.AuthPrincipal;

import java.util.Optional;

public interface AuthTokenPort {
    String createAccessToken(Long accountId, AccountRole role);
    String createRefreshToken(Long accountId, AccountRole role);
    Optional<AuthPrincipal> parseAccessToken(String token);
    Optional<AuthPrincipal> parseRefreshToken(String token);
}
