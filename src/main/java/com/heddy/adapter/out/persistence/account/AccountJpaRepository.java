package com.heddy.adapter.out.persistence.account;

import com.heddy.domain.account.model.AuthProvider;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface AccountJpaRepository extends JpaRepository<AccountEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT account FROM AccountEntity account WHERE account.userId = :userId")
    Optional<AccountEntity> findByIdForUpdate(@Param("userId") UUID userId);

    Optional<AccountEntity> findByEmail(String email);
    Optional<AccountEntity> findByAuthProviderAndProviderSubject(
            AuthProvider authProvider, String providerSubject);
    boolean existsByEmail(String email);
}
