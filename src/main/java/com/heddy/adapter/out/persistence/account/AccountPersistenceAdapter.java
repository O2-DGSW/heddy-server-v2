package com.heddy.adapter.out.persistence.account;

import com.heddy.domain.account.model.Account;
import com.heddy.domain.account.model.AuthProvider;
import com.heddy.domain.account.model.ConsentDecision;
import com.heddy.domain.account.model.ConsentSource;
import com.heddy.domain.account.model.RefreshSession;
import com.heddy.domain.account.model.UserProfile;
import com.heddy.domain.account.port.out.AccountRepositoryPort;
import com.heddy.domain.account.port.out.ConsentHistoryRepositoryPort;
import com.heddy.domain.account.port.out.RefreshSessionRepositoryPort;
import com.heddy.domain.account.port.out.UserProfileRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AccountPersistenceAdapter implements
        AccountRepositoryPort,
        UserProfileRepositoryPort,
        ConsentHistoryRepositoryPort,
        RefreshSessionRepositoryPort {

    private final AccountJpaRepository accountJpaRepository;
    private final UserProfileJpaRepository userProfileJpaRepository;
    private final ConsentHistoryJpaRepository consentHistoryJpaRepository;
    private final RefreshTokenJpaRepository refreshTokenJpaRepository;

    @Override
    public Account save(Account account) {
        AccountEntity entity = accountJpaRepository.findById(account.userId())
                .orElseGet(() -> new AccountEntity(account));
        entity.update(account);
        return accountJpaRepository.save(entity).toDomain();
    }

    @Override
    public Optional<Account> findById(UUID userId) {
        return accountJpaRepository.findById(userId).map(AccountEntity::toDomain);
    }

    @Override
    public Optional<Account> findByIdForUpdate(UUID userId) {
        return accountJpaRepository.findByIdForUpdate(userId).map(AccountEntity::toDomain);
    }

    @Override
    public Optional<Account> findByEmail(String email) {
        return accountJpaRepository.findByEmail(email).map(AccountEntity::toDomain);
    }

    @Override
    public Optional<Account> findByProvider(AuthProvider provider, String providerSubject) {
        return accountJpaRepository.findByAuthProviderAndProviderSubject(provider, providerSubject)
                .map(AccountEntity::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return accountJpaRepository.existsByEmail(email);
    }

    @Override
    public void updatePassword(UUID userId, String passwordHash) {
        AccountEntity entity = accountJpaRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("AccountEntity not found: " + userId));
        entity.updatePassword(passwordHash);
    }

    @Override
    public UserProfile save(UserProfile profile) {
        UserProfileEntity entity = userProfileJpaRepository.findById(profile.userId())
                .orElseGet(() -> new UserProfileEntity(profile));
        entity.update(profile);
        return userProfileJpaRepository.saveAndFlush(entity).toDomain();
    }

    @Override
    public Optional<UserProfile> findByUserId(UUID userId) {
        return userProfileJpaRepository.findById(userId).map(UserProfileEntity::toDomain);
    }

    @Override
    public Optional<UUID> findUserIdByPhone(String phone) {
        return userProfileJpaRepository.findByPhone(phone).map(UserProfileEntity::toDomain)
                .map(UserProfile::userId);
    }

    @Override
    public boolean existsByPhone(String phone) {
        return userProfileJpaRepository.existsByPhone(phone);
    }

    @Override
    public void append(
            UUID userId,
            List<ConsentDecision> decisions,
            ConsentSource source,
            Instant changedAt
    ) {
        consentHistoryJpaRepository.saveAll(decisions.stream()
                .map(decision -> new ConsentHistoryEntity(userId, decision, source, changedAt))
                .toList());
    }

    @Override
    public RefreshSession save(RefreshSession session) {
        return refreshTokenJpaRepository.save(new RefreshTokenEntity(session)).toDomain();
    }

    @Override
    public Optional<RefreshSession> findByTokenHashForUpdate(String tokenHash) {
        return refreshTokenJpaRepository.findByTokenHash(tokenHash).map(RefreshTokenEntity::toDomain);
    }

    @Override
    public void rotate(UUID currentId, UUID nextId, Instant revokedAt) {
        refreshTokenJpaRepository.findById(currentId)
                .orElseThrow(() -> new IllegalStateException("RefreshTokenEntity not found: " + currentId))
                .rotate(nextId, revokedAt);
    }

    @Override
    public void revoke(UUID refreshTokenId, Instant revokedAt) {
        refreshTokenJpaRepository.findById(refreshTokenId).ifPresent(entity -> entity.revoke(revokedAt));
    }

    @Override
    public void revokeAll(UUID userId, Instant revokedAt) {
        refreshTokenJpaRepository.findAllByUserIdAndRevokedAtIsNull(userId)
                .forEach(entity -> entity.revoke(revokedAt));
    }
}
