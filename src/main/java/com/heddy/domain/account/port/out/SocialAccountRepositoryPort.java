package com.heddy.domain.account.port.out;

import com.heddy.domain.account.model.Account;
import com.heddy.domain.account.model.SocialProvider;

import java.util.Optional;

public interface SocialAccountRepositoryPort {
    Optional<Account> findByProvider(SocialProvider provider, String providerId);
    void link(Long accountId, SocialProvider provider, String providerId);
}
