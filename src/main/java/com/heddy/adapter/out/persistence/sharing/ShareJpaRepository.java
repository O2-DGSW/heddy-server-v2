package com.heddy.adapter.out.persistence.sharing;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface ShareJpaRepository extends JpaRepository<ShareEntity, UUID> {

    Optional<ShareEntity> findByShareIdAndUserId(UUID shareId, UUID userId);

    Optional<ShareEntity> findByTokenHash(String tokenHash);

    Page<ShareEntity> findByUserId(UUID userId, Pageable pageable);

    Page<ShareEntity> findByUserIdAndStatus(UUID userId, String status, Pageable pageable);
}
