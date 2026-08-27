package com.heddy.domain.account.port.in;

import com.heddy.domain.account.model.AuthProvider;

public record SocialLoginCommand(
        AuthProvider provider,
        String providerToken
) {
}
