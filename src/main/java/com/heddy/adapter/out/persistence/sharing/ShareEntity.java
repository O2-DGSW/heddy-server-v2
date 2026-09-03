package com.heddy.adapter.out.persistence.sharing;

import com.heddy.adapter.out.persistence.BaseEntity;
import com.heddy.domain.sharing.exception.SharingError;
import com.heddy.domain.sharing.exception.SharingException;
import com.heddy.domain.sharing.model.Share;
import com.heddy.domain.sharing.model.ShareFieldType;
import com.heddy.domain.sharing.model.ShareStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 공유의 JPA 표현. 선택된 기록·항목·후보는 조인 테이블 3개를 {@code @ElementCollection} 으로
 * 붙인다 — 자체 생애주기를 갖지 않는 순수 연결이라 엔티티로 승격할 이유가 없다.
 *
 * <p>열거형을 곧바로 묶지 않고 이름으로만 저장하는 이유는 시술기록의 service_types 와 같다.
 * 행에서 알 수 없는 이름이 읽힐 때 직렬화 계층의 예외 대신 도메인 오류로 막는다.
 */
@Entity
@Table(name = "shares")
class ShareEntity extends BaseEntity {

    @Id
    @Column(name = "share_id", nullable = false, updatable = false)
    private UUID shareId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, unique = true, updatable = false, length = 64)
    private String tokenHash;

    /**
     * 대상 구성의 SHA-256. 두 조인 테이블에 흩어진 대상을 shares 한 테이블에서 비교하려고
     * 비정규화한 인덱스 키다(V31). 대상은 발급 후 바뀌지 않으므로 갱신하지 않는다.
     */
    @Column(name = "target_hash", nullable = false, updatable = false, length = 64)
    private String targetHash;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "share_records", joinColumns = @JoinColumn(name = "share_id"))
    @Column(name = "record_id")
    private Set<UUID> recordIds = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "share_fields", joinColumns = @JoinColumn(name = "share_id"))
    @Column(name = "field_type")
    private Set<String> fieldTypes = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "share_saved_styles", joinColumns = @JoinColumn(name = "share_id"))
    @Column(name = "saved_style_id")
    private Set<UUID> savedStyleIds = new LinkedHashSet<>();

    protected ShareEntity() {
    }

    ShareEntity(Share share, String targetHash) {
        this.targetHash = targetHash;
        shareId = share.shareId();
        userId = share.userId();
        tokenHash = share.tokenHash();
        status = share.status().name();
        expiresAt = share.expiresAt();
        revokedAt = share.revokedAt();
        recordIds = new LinkedHashSet<>(share.recordIds());
        fieldTypes = share.fields().stream()
                .map(Enum::name)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        savedStyleIds = new LinkedHashSet<>(share.savedStyleIds());
    }

    Share toDomain() {
        return new Share(
                shareId, userId, tokenHash, parseStatus(), expiresAt, revokedAt,
                Set.copyOf(recordIds), parseFieldTypes(), Set.copyOf(savedStyleIds),
                getCreatedAt());
    }

    /** 노출 항목 교체(PATCH). 대상(기록·후보) 조인 행은 수정 API 의 범위가 아니다. */
    void updateFields(Set<ShareFieldType> fields) {
        fieldTypes = fields.stream()
                .map(Enum::name)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** 만료 시각 수정(PATCH). 과거 값 거부는 도메인이 이미 했다는 전제다. */
    void updateExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    void updateStatus(String statusName, Instant revokedAt) {
        this.status = statusName;
        this.revokedAt = revokedAt;
    }

    private ShareStatus parseStatus() {
        try {
            return ShareStatus.valueOf(status);
        } catch (IllegalArgumentException invalidName) {
            throw new SharingException(SharingError.FIELD_UNKNOWN);
        }
    }

    private Set<ShareFieldType> parseFieldTypes() {
        return fieldTypes.stream()
                .map(ShareEntity::parseFieldType)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static ShareFieldType parseFieldType(String name) {
        try {
            return ShareFieldType.valueOf(name);
        } catch (IllegalArgumentException invalidName) {
            throw new SharingException(SharingError.FIELD_UNKNOWN);
        }
    }
}
