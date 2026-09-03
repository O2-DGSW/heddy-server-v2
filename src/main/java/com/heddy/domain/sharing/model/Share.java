package com.heddy.domain.sharing.model;

import com.heddy.domain.sharing.exception.SharingError;
import com.heddy.domain.sharing.exception.SharingException;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;

/**
 * 공유 링크 한 건과 선택된 대상·항목. 공유 도메인의 애그리게이트 루트다.
 *
 * <p>불변식은 이 모델만 책임진다(스펙 11.2 Validation).
 * <ul>
 *   <li>기록 1개 이상 또는 후보 스타일 1개 이상 — 빈 링크는 성립하지 않는다</li>
 *   <li>공유 항목 1개 이상 — 아무것도 보여주지 않는 링크도 성립하지 않는다</li>
 *   <li>만료 시각은 항상 미래 — 생성·수정 어느 쪽이든 과거로 만들 수 없다</li>
 * </ul>
 *
 * <p>대상의 소유권 검증은 여기서 하지 않는다. 도메인은 소유자를 알지만 타인의 기록 목록을
 * 볼 수 없으므로, 유스케이스(#49)가 저장 계층 질의로 검증한다.
 *
 * <p>토큰은 원문을 받지 않고 해시만 받는다. 원문은 응답으로 딱 한 번 내보낼 값이며 서버 어디에도
 * 저장되지 않아야 한다(스펙 19절). 만료 판정은 상태가 아니라 {@link #isExpired} 비교로 매 요청
 * 한다 — 만료를 상태로 두면 갱신 작업이 필요해지기 때문이다.
 */
public record Share(
        UUID shareId,
        UUID userId,
        String tokenHash,
        ShareStatus status,
        Instant expiresAt,
        Instant revokedAt,
        Set<UUID> recordIds,
        Set<ShareFieldType> fields,
        Set<UUID> savedStyleIds,
        Instant createdAt
) {
    public static final int DEFAULT_EXPIRES_IN_DAYS = 7;

    public Share {
        Objects.requireNonNull(shareId, "shareId");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(status, "status");

        recordIds = Set.copyOf(recordIds);
        savedStyleIds = Set.copyOf(savedStyleIds);
        fields = Set.copyOf(fields);
        // 스펙 오류(SHARE_EMPTY_SELECTION)가 기술 전제(TOKEN_HASH_REQUIRED)에 가려지지 않게
        // 선택 불변식을 먼저 본다.
        if (recordIds.isEmpty() && savedStyleIds.isEmpty()) {
            throw new SharingException(SharingError.EMPTY_SELECTION);
        }
        if (fields.isEmpty()) {
            throw new SharingException(SharingError.EMPTY_SELECTION);
        }
        if (tokenHash == null || tokenHash.isBlank()) {
            throw new SharingException(SharingError.TOKEN_HASH_REQUIRED);
        }

        Objects.requireNonNull(expiresAt, "expiresAt");
    }

    /**
     * 새 공유를 만든다. 식별자와 토큰 해시는 호출부가 준비해 온다 — 토큰 발급·해싱은 인프라
     * 관심이라 도메인이 알지 않는다. 유효기간을 비우면 기본 7일(스펙 11.2)이다.
     */
    public static Share create(
            UUID userId,
            String tokenHash,
            Set<UUID> recordIds,
            Set<UUID> savedStyleIds,
            Set<ShareFieldType> fields,
            Integer expiresInDays,
            Instant now
    ) {
        int days = expiresInDays == null ? DEFAULT_EXPIRES_IN_DAYS : expiresInDays;
        if (days < 1) {
            throw new SharingException(SharingError.EXPIRES_IN_DAYS_INVALID);
        }
        return new Share(UUID.randomUUID(), userId, tokenHash, ShareStatus.ACTIVE,
                now.plusSeconds((long) days * 86_400), null, recordIds, fields, savedStyleIds, now);
    }

    /**
     * 대상 구성의 정규형. 같은 기록·후보를 고른 두 공유는 고르는 순서와 무관하게 같은 문자열을
     * 낸다. 해싱은 이 문자열을 받는 쪽의 일이고, 여기서는 "무엇이 같은 대상인가" 만 정한다.
     *
     * <p>정렬은 UUID 문자열 기준이다. V31 의 백필도 같은 기준을 쓴다 — 두 정렬이 어긋나면
     * 같은 대상이 서로 다른 해시를 갖고 중복 제거가 통째로 무력해진다.
     */
    public String targetKey() {
        return join(recordIds) + "|" + join(savedStyleIds);
    }

    private static String join(Set<UUID> ids) {
        return ids.stream().map(UUID::toString).sorted().collect(Collectors.joining(","));
    }

    /** 이미 읽어 온 행을 도메인으로 되돌릴 때 쓰는 재구성용 팩터리다. 불변식을 다시 통과한다. */
    public static Share reconstitute(
            UUID shareId,
            UUID userId,
            String tokenHash,
            ShareStatus status,
            Instant expiresAt,
            Instant revokedAt,
            Set<UUID> recordIds,
            Set<ShareFieldType> fields,
            Set<UUID> savedStyleIds,
            Instant createdAt
    ) {
        return new Share(shareId, userId, tokenHash, status, expiresAt, revokedAt,
                recordIds, fields, savedStyleIds, createdAt);
    }

    /** 만료 여부. 철회·만료 모두 매 요청 검증 대상(스펙 19절)이라 시각을 반드시 받는다. */
    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    /** 공개 조회가 가능한 상태인지. 철회와 만료를 함께 본다. */
    public boolean isViewable(Instant now) {
        return status == ShareStatus.ACTIVE && !isExpired(now);
    }

    /**
     * 즉시 철회한다. 이미 철회된 공유는 그대로 돌려보내는 멱등 동작이다 — DELETE 는 204 로
     * 끝나야 하고 두 번 눌러 실패하면 안 된다.
     */
    public Share revoke(Instant now) {
        if (status == ShareStatus.REVOKED) {
            return this;
        }
        return new Share(shareId, userId, tokenHash, ShareStatus.REVOKED, expiresAt,
                now, recordIds, fields, savedStyleIds, createdAt);
    }

    /**
     * 노출 항목과 만료 시각을 바꾼 새 스냅샷을 반환한다(PATCH /shares/{shareId}). 대상(기록·후보)
     * 는 수정 대상이 아니다 — 스펙 11.4 가 허용하는 필드는 둘뿐이다.
     */
    public Share update(Set<ShareFieldType> fields, Instant expiresAt, Instant now) {
        Objects.requireNonNull(expiresAt, "expiresAt");
        if (!expiresAt.isAfter(now)) {
            throw new SharingException(SharingError.EXPIRES_AT_NOT_FUTURE);
        }
        return new Share(shareId, userId, tokenHash, status, expiresAt, revokedAt,
                recordIds, Set.copyOf(fields), savedStyleIds, createdAt);
    }
}
