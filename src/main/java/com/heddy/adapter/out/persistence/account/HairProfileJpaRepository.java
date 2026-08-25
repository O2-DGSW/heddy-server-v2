package com.heddy.adapter.out.persistence.account;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface HairProfileJpaRepository extends JpaRepository<HairProfileEntity, UUID> {
    Optional<HairProfileEntity> findByUserId(UUID userId);
    long deleteByUserId(UUID userId);
}
