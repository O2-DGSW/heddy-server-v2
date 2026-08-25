package com.heddy.adapter.out.persistence.account;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface UserProfileJpaRepository extends JpaRepository<UserProfileEntity, UUID> {
    Optional<UserProfileEntity> findByPhone(String phone);
    boolean existsByPhone(String phone);
    long deleteByUserId(UUID userId);
}
