package com.heddy.domain.account.port.out;

import com.heddy.domain.account.model.AuthProvider;

import java.util.Optional;

public interface SocialTokenVerifierPort {
    Optional<VerifiedSocialIdentity> verify(AuthProvider provider, String providerToken);

    interface VerifiedSocialIdentity {
        String subject();
    }
}
