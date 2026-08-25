package com.heddy.adapter.out.persistence.account;

import com.heddy.domain.account.model.AccountDeletionStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface AccountDeletionRequestJpaRepository
        extends JpaRepository<AccountDeletionRequestEntity, UUID> {

    Optional<AccountDeletionRequestEntity> findFirstByUserIdAndStatusOrderByRequestedAtDesc(
            UUID userId, AccountDeletionStatus status);

    List<AccountDeletionRequestEntity> findAllByStatusOrderByRequestedAtAsc(
            AccountDeletionStatus status, Pageable pageable);

    @Query("""
            SELECT r FROM AccountDeletionRequestEntity r
            WHERE r.status = :status
                AND r.attemptCount < :maxAttempts
                AND COALESCE(r.completedAt, r.requestedAt) <= :retryCutoff
            ORDER BY r.requestedAt ASC
            """)
    List<AccountDeletionRequestEntity> findRetryBatch(
            @Param("status") AccountDeletionStatus status,
            @Param("maxAttempts") int maxAttempts,
            @Param("retryCutoff") Instant retryCutoff,
            Pageable pageable);
}
