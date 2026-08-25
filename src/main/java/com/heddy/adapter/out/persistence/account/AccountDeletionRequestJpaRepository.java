package com.heddy.adapter.out.persistence.account;

import com.heddy.domain.account.model.AccountDeletionStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface AccountDeletionRequestJpaRepository
        extends JpaRepository<AccountDeletionRequestEntity, UUID> {

    Optional<AccountDeletionRequestEntity> findFirstByUserIdAndStatusOrderByRequestedAtDesc(
            UUID userId, AccountDeletionStatus status);

    List<AccountDeletionRequestEntity> findAllByStatusOrderByRequestedAtAsc(
            AccountDeletionStatus status, Pageable pageable);
}
