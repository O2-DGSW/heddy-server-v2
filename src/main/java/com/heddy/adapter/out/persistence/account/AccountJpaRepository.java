package com.heddy.adapter.out.persistence.account;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface AccountJpaRepository extends JpaRepository<AccountEntity, Long> {
    Optional<AccountEntity> findByLoginId(String loginId);
    Optional<AccountEntity> findByPhoneNumber(String phoneNumber);
    boolean existsByLoginId(String loginId);
    boolean existsByPhoneNumber(String phoneNumber);
}
