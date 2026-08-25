package com.heddy.adapter.out.persistence.account;

import com.heddy.domain.account.model.AccountDeletionRequest;
import com.heddy.domain.account.model.AccountDeletionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "account_deletion_requests")
class AccountDeletionRequestEntity {

    @Id
    @Column(name = "deletion_request_id", nullable = false)
    private UUID deletionRequestId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountDeletionStatus status;

    @Column(length = 255)
    private String reason;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected AccountDeletionRequestEntity() {
    }

    AccountDeletionRequestEntity(AccountDeletionRequest request) {
        update(request);
    }

    void update(AccountDeletionRequest request) {
        deletionRequestId = request.deletionRequestId();
        userId = request.userId();
        status = request.status();
        reason = request.reason();
        requestedAt = request.requestedAt();
        completedAt = request.completedAt();
    }

    AccountDeletionRequest toDomain() {
        return new AccountDeletionRequest(
                deletionRequestId, userId, status, reason, requestedAt, completedAt);
    }
}
