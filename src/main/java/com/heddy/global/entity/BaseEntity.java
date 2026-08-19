package com.heddy.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;

/**
 * 생성·수정 시각을 갖는 상위 타입. 갱신되는 테이블은 이쪽을 상속한다.
 */
@MappedSuperclass
public abstract class BaseEntity extends BaseCreatedEntity {

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
