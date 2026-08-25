package com.heddy.adapter.out.persistence.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

interface UsedReauthenticationTokenJpaRepository
        extends JpaRepository<UsedReauthenticationTokenEntity, UUID> {

    @Modifying
    @Query("DELETE FROM UsedReauthenticationTokenEntity t WHERE t.usedAt < :threshold")
    int deleteByUsedAtBefore(@Param("threshold") Instant threshold);
}
