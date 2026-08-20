package com.heddy.domain.account.port.in;

import com.heddy.domain.account.model.AuthProvider;

import java.util.UUID;

public record ReauthenticateCommand(
        UUID userId,
        Method method,
        String password,
        AuthProvider provider,
        String providerToken
) {
    public enum Method {
        PASSWORD,
        SOCIAL_TOKEN
    }
}
