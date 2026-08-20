package com.heddy.adapter.out.persistence.account;

import com.heddy.domain.account.model.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface AccountJpaRepository extends JpaRepository<AccountEntity, UUID> {
    Optional<AccountEntity> findByEmail(String email);
    Optional<AccountEntity> findByAuthProviderAndProviderSubject(
            AuthProvider authProvider, String providerSubject);
    boolean existsByEmail(String email);
}
