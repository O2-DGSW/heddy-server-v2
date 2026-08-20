package com.heddy.domain.account.port.in;

import com.heddy.domain.account.model.AuthProvider;
import com.heddy.domain.account.model.ConsentDecision;

import java.util.List;

public record SocialSignupCommand(
        AuthProvider provider,
        String providerToken,
        String nickname,
        String phone,
        List<ConsentDecision> agreements
) {
}
