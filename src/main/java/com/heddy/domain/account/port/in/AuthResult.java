package com.heddy.domain.account.port.in;

public record AuthResult(AuthUser user, AuthTokens tokens) {
}
