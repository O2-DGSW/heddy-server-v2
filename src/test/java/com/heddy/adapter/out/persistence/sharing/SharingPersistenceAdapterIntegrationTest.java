package com.heddy.adapter.out.persistence.sharing;

import com.heddy.domain.sharing.model.Share;
import com.heddy.domain.sharing.model.ShareFieldType;
import com.heddy.domain.sharing.model.ShareStatus;
import com.heddy.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 목록의 "공유중" 배지가 쓰는 조회. 배지 판정이 공개 조회와 어긋나면 철회했거나 만료된 링크가
 * 계속 공유중으로 보이므로, 상태별로 갈라지는지 실제 스키마 위에서 확인한다.
 */
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SharingPersistenceAdapterIntegrationTest extends PostgresIntegrationTest {

    private static final UUID OWNER_ID = UUID.fromString("83000000-0000-4000-8000-000000000001");
    private static final UUID OTHER_USER_ID = UUID.fromString("83000000-0000-4000-8000-000000000002");
    private static final Instant NOW = Instant.parse("2026-08-30T10:00:00Z");

    @Autowired SharingPersistenceAdapter adapter;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpUsers() {
        insertUser(OWNER_ID, "share-owner@example.com");
        insertUser(OTHER_USER_ID, "share-other@example.com");
    }

    @Test
    void findsRecordsHeldByAnActiveUnexpiredShare() {
        UUID recordId = insertRecord(OWNER_ID);
        insertShare(OWNER_ID, recordId, ShareStatus.ACTIVE, NOW.plusSeconds(60));

        assertThat(adapter.findSharedRecordIds(OWNER_ID, List.of(recordId), NOW))
                .containsExactly(recordId);
    }

    /** 만료는 상태로 저장되지 않는다. status 만 보면 만료된 링크가 여기서 공유중으로 남는다. */
    @Test
    void ignoresSharesWhoseExpiryHasPassed() {
        UUID recordId = insertRecord(OWNER_ID);
        insertShare(OWNER_ID, recordId, ShareStatus.ACTIVE, NOW.minusSeconds(1));

        assertThat(adapter.findSharedRecordIds(OWNER_ID, List.of(recordId), NOW)).isEmpty();
    }

    /** 만료 시각이 기준 시각과 같으면 이미 만료다 — 공개 조회(Share#isViewable)와 같은 경계다. */
    @Test
    void treatsAnExpiryEqualToNowAsExpired() {
        UUID recordId = insertRecord(OWNER_ID);
        insertShare(OWNER_ID, recordId, ShareStatus.ACTIVE, NOW);

        assertThat(adapter.findSharedRecordIds(OWNER_ID, List.of(recordId), NOW)).isEmpty();
    }

    @Test
    void ignoresRevokedShares() {
        UUID recordId = insertRecord(OWNER_ID);
        insertShare(OWNER_ID, recordId, ShareStatus.REVOKED, NOW.plusSeconds(60));

        assertThat(adapter.findSharedRecordIds(OWNER_ID, List.of(recordId), NOW)).isEmpty();
    }

    @Test
    void returnsNothingForRecordsThatWereNeverShared() {
        UUID recordId = insertRecord(OWNER_ID);

        assertThat(adapter.findSharedRecordIds(OWNER_ID, List.of(recordId), NOW)).isEmpty();
    }

    /** 남의 공유가 내 배지를 켜지 못한다. 소유자 조건이 빠지면 이 테스트가 잡는다. */
    @Test
    void ignoresSharesOwnedBySomeoneElse() {
        UUID recordId = insertRecord(OWNER_ID);
        insertShare(OTHER_USER_ID, recordId, ShareStatus.ACTIVE, NOW.plusSeconds(60));

        assertThat(adapter.findSharedRecordIds(OWNER_ID, List.of(recordId), NOW)).isEmpty();
    }

    /** 페이지를 한 번에 묻는 자리다. 공유된 것만 골라 돌려주고 나머지는 담기지 않는다. */
    @Test
    void answersForAWholePageInOneCall() {
        UUID shared = insertRecord(OWNER_ID);
        UUID notShared = insertRecord(OWNER_ID);
        insertShare(OWNER_ID, shared, ShareStatus.ACTIVE, NOW.plusSeconds(60));

        assertThat(adapter.findSharedRecordIds(OWNER_ID, List.of(shared, notShared), NOW))
                .containsExactly(shared);
    }

    /** 빈 IN 절은 방언에 따라 문법 오류가 된다. 질의 없이 답이 나와야 한다. */
    @Test
    void answersWithoutQueryingWhenThereAreNoRecords() {
        assertThat(adapter.findSharedRecordIds(OWNER_ID, List.of(), NOW)).isEmpty();
    }

    // ------------------------------------------------------------------ 헬퍼

    private void insertUser(UUID userId, String email) {
        jdbcTemplate.update("""
                INSERT INTO users (
                    user_id, email, password_hash, auth_provider, status, login_fail_count
                ) VALUES (?, ?, ?, 'EMAIL', 'ACTIVE', 0)
                """, userId, email, "hash");
    }

    private UUID insertRecord(UUID ownerId) {
        UUID recordId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO treatment_records (record_id, user_id, service_types, performed_at)
                VALUES (?, ?, ?::jsonb, ?)
                """, recordId, ownerId, "[\"CUT\"]", Timestamp.from(NOW.minusSeconds(3600)));
        return recordId;
    }

    private void insertShare(UUID ownerId, UUID recordId, ShareStatus status, Instant expiresAt) {
        adapter.insert(Share.reconstitute(
                UUID.randomUUID(), ownerId, UUID.randomUUID().toString().replace("-", ""),
                status, expiresAt, status == ShareStatus.REVOKED ? NOW.minusSeconds(10) : null,
                Set.of(recordId), Set.of(ShareFieldType.PHOTOS), Set.of(), NOW));
    }
}
