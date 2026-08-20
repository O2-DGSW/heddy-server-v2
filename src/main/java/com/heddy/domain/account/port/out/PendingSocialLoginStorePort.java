package com.heddy.domain.account.port.out;

import com.heddy.domain.account.model.SocialProvider;

import java.util.Optional;

public interface PendingSocialLoginStorePort {
    void save(String pendingToken, SocialProvider provider, String providerId);
    Optional<PendingSocialLogin> find(String pendingToken);
    void delete(String pendingToken);

    interface PendingSocialLogin {
        SocialProvider provider();
        String providerId();
    }
}
