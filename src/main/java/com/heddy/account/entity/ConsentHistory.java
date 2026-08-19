package com.heddy.account.entity;

import com.heddy.global.entity.BaseCreatedEntity;
import com.heddy.global.support.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * 동의 이력. append-only — 동의 상태가 바뀌면 기존 행을 수정하지 않고 새 행을 추가한다.
 * 현재 상태는 (user, consentType) 기준 created_at 이 가장 큰 행이다.
 * 갱신이 없으므로 updated_at 을 두지 않는다 — {@link BaseCreatedEntity} 를 상속한다.
 */
@Entity
@Table(name = "consent_history")
public class ConsentHistory extends BaseCreatedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_type", nullable = false, updatable = false, length = 30)
    private ConsentType consentType;

    @Column(name = "agreed", nullable = false, updatable = false)
    private boolean agreed;

    @Column(name = "policy_version", nullable = false, updatable = false, length = 20)
    private String policyVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, updatable = false, length = 20)
    private ConsentSource source;

    protected ConsentHistory() {
    }

    public ConsentHistory(User user, ConsentType consentType, boolean agreed,
                          String policyVersion, ConsentSource source) {
        this.id = UuidV7.generate();
        this.user = user;
        this.consentType = consentType;
        this.agreed = agreed;
        this.policyVersion = policyVersion;
        this.source = source;
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public ConsentType getConsentType() {
        return consentType;
    }

    public boolean isAgreed() {
        return agreed;
    }

    public String getPolicyVersion() {
        return policyVersion;
    }

    public ConsentSource getSource() {
        return source;
    }
}
