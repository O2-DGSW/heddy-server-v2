package com.heddy.domain.account.port.out;

import com.heddy.domain.account.model.AuthPrincipal;

import java.util.Optional;
import java.util.UUID;

public interface AuthTokenPort {
    String createAccessToken(UUID userId);
    String createReauthenticationToken(UUID userId);
    Optional<AuthPrincipal> parseAccessToken(String token);
}
