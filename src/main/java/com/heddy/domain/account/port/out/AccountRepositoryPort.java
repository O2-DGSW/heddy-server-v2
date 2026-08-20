package com.heddy.domain.account.port.out;

import com.heddy.domain.account.model.Account;
import com.heddy.domain.account.model.AuthProvider;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepositoryPort {
    Account save(Account account);
    Optional<Account> findById(UUID userId);
    Optional<Account> findByEmail(String email);
    Optional<Account> findByProvider(AuthProvider provider, String providerSubject);
    boolean existsByEmail(String email);
    void updatePassword(UUID userId, String passwordHash);
}
