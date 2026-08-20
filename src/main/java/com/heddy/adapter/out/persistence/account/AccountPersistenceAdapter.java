package com.heddy.adapter.out.persistence.account;

import com.heddy.domain.account.model.Account;
import com.heddy.domain.account.model.SocialProvider;
import com.heddy.domain.account.port.out.AccountRepositoryPort;
import com.heddy.domain.account.port.out.SocialAccountRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AccountPersistenceAdapter implements AccountRepositoryPort, SocialAccountRepositoryPort {

    private final AccountJpaRepository accountJpaRepository;
    private final SocialAccountJpaRepository socialAccountJpaRepository;

    @Override
    public Account save(Account account) {
        AccountEntity entity = new AccountEntity(
                account.loginId(),
                account.encodedPassword(),
                account.name(),
                account.phoneNumber(),
                account.role(),
                account.status(),
                account.phoneVerified());
        return toDomain(accountJpaRepository.save(entity));
    }

    @Override
    public Optional<Account> findById(Long accountId) {
        return accountJpaRepository.findById(accountId).map(this::toDomain);
    }

    @Override
    public Optional<Account> findByLoginId(String loginId) {
        return accountJpaRepository.findByLoginId(loginId).map(this::toDomain);
    }

    @Override
    public Optional<Account> findByPhoneNumber(String phoneNumber) {
        return accountJpaRepository.findByPhoneNumber(phoneNumber).map(this::toDomain);
    }

    @Override
    public boolean existsByLoginId(String loginId) {
        return accountJpaRepository.existsByLoginId(loginId);
    }

    @Override
    public boolean existsByPhoneNumber(String phoneNumber) {
        return accountJpaRepository.existsByPhoneNumber(phoneNumber);
    }

    @Override
    public void updatePassword(Long accountId, String encodedPassword) {
        AccountEntity entity = accountJpaRepository.findById(accountId)
                .orElseThrow(() -> new IllegalStateException("AccountEntity not found: " + accountId));
        entity.updatePassword(encodedPassword);
    }

    @Override
    public Optional<Account> findByProvider(SocialProvider provider, String providerId) {
        return socialAccountJpaRepository.findByProviderAndProviderId(provider, providerId)
                .map(SocialAccountEntity::account)
                .map(this::toDomain);
    }

    @Override
    public void link(Long accountId, SocialProvider provider, String providerId) {
        AccountEntity account = accountJpaRepository.findById(accountId)
                .orElseThrow(() -> new IllegalStateException("AccountEntity not found: " + accountId));
        socialAccountJpaRepository.save(new SocialAccountEntity(account, provider, providerId));
    }

    private Account toDomain(AccountEntity entity) {
        return new Account(
                entity.id(),
                entity.loginId(),
                entity.encodedPassword(),
                entity.name(),
                entity.phoneNumber(),
                entity.role(),
                entity.status(),
                entity.phoneVerified());
    }
}
