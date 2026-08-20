package com.heddy.domain.account.port.out;

import com.heddy.domain.account.model.Account;

import java.util.Optional;

public interface AccountRepositoryPort {
    Account save(Account account);
    Optional<Account> findById(Long accountId);
    Optional<Account> findByLoginId(String loginId);
    Optional<Account> findByPhoneNumber(String phoneNumber);
    boolean existsByLoginId(String loginId);
    boolean existsByPhoneNumber(String phoneNumber);
    void updatePassword(Long accountId, String encodedPassword);
}
