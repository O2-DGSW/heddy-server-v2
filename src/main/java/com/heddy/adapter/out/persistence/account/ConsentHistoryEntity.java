package com.heddy.adapter.out.persistence.account;

import com.heddy.domain.account.model.ConsentDecision;
import com.heddy.domain.account.model.ConsentSource;
import com.heddy.domain.account.model.ConsentStatus;
import com.heddy.domain.account.model.ConsentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "consent_history")
class ConsentHistoryEntity {

    @Id
    @Column(name = "consent_id", nullable = false)
    private UUID consentId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_type", nullable = false, length = 30)
    private ConsentType consentType;

    @Column(nullable = false)
    private boolean granted;

    @Column(name = "policy_version", nullable = false, length = 20)
    private String policyVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConsentSource source;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    protected ConsentHistoryEntity() {
    }

    ConsentHistoryEntity(
            UUID userId,
            ConsentDecision decision,
            ConsentSource source,
            Instant changedAt
    ) {
        consentId = UUID.randomUUID();
        this.userId = userId;
        consentType = decision.type();
        granted = decision.granted();
        policyVersion = decision.policyVersion();
        this.source = source;
        this.changedAt = changedAt;
    }

    ConsentStatus toDomain() {
        return new ConsentStatus(
                userId, consentType, granted, policyVersion, source, changedAt);
    }
}
