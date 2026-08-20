package com.heddy.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * 생성 시각만 갖는 상위 타입. append-only 테이블은 갱신되지 않으므로 이쪽을 상속한다.
 * 갱신되는 테이블은 updated_at 을 더한 {@link BaseEntity} 를 상속한다.
 *
 * <p>PK 는 DB 시퀀스가 아니라 애플리케이션이 UUIDv7 로 채운다. 그래서 기본 판정
 * (id == null 이면 신규)이 항상 "기존 행"으로 나와 {@code save()} 가 SELECT 를 한 번
 * 더 태우는 {@code merge()} 경로를 타고, 반환된 복사본이 아닌 원본은 detached 로 남아
 * 감사 필드가 채워지지 않는다. {@link Persistable} 로 신규 여부를 직접 알려 이를 막는다.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseCreatedEntity implements Persistable<UUID> {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Transient
    private boolean isNew = true;

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostPersist
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }
}
