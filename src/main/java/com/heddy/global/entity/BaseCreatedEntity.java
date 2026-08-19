package com.heddy.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * 생성 시각만 갖는 상위 타입. append-only 테이블은 갱신되지 않으므로 이쪽을 상속한다.
 * 갱신되는 테이블은 updated_at 을 더한 {@link BaseEntity} 를 상속한다.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseCreatedEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Instant getCreatedAt() {
        return createdAt;
    }
}
