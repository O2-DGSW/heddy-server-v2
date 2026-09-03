package com.heddy.adapter.out.persistence.summary;

import com.heddy.domain.summary.model.MySummary;
import com.heddy.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 요약 카운트의 정의가 전부 SQL 에 있어 실제 스키마 위에서 본다. 특히 분석·공유는 행이 아니라
 * 기록 기준 distinct 여야 한다 — 행을 세면 시술 기록 수보다 큰 값이 나온다.
 */
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MySummaryJdbcAdapterIntegrationTest extends PostgresIntegrationTest {

    private static final UUID OWNER_ID = UUID.fromString("84000000-0000-4000-8000-000000000001");
    private static final UUID OTHER_USER_ID = UUID.fromString("84000000-0000-4000-8000-000000000002");
    private static final Instant NOW = Instant.parse("2026-08-30T10:00:00Z");

    @Autowired MySummaryJdbcAdapter adapter;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpUsers() {
        insertUser(OWNER_ID, "summary-owner@example.com");
        insertUser(OTHER_USER_ID, "summary-other@example.com");
    }

    @Test
    void answersZeroForEveryCountWhenThereIsNothing() {
        assertThat(adapter.count(OWNER_ID, NOW))
                .isEqualTo(new MySummary(0, 0, 0, 0));
    }

    /** 같은 기록으로 링크를 여러 번 만들어도 기록은 하나다. 여기서 3 이 나오면 정의가 틀린 것. */
    @Test
    void countsARecordOnceEvenWhenSeveralLinksPointAtIt() {
        UUID recordId = insertRecord(OWNER_ID);
        insertShare(OWNER_ID, recordId, "ACTIVE", NOW.plusSeconds(60));
        insertShare(OWNER_ID, recordId, "ACTIVE", NOW.plusSeconds(120));
        insertShare(OWNER_ID, recordId, "ACTIVE", NOW.plusSeconds(180));

        assertThat(adapter.count(OWNER_ID, NOW).sharedRecordCount()).isEqualTo(1);
    }

    @Test
    void excludesRecordsHeldOnlyByRevokedOrExpiredShares() {
        insertShare(OWNER_ID, insertRecord(OWNER_ID), "REVOKED", NOW.plusSeconds(60));
        insertShare(OWNER_ID, insertRecord(OWNER_ID), "ACTIVE", NOW.minusSeconds(1));
        UUID live = insertRecord(OWNER_ID);
        insertShare(OWNER_ID, live, "ACTIVE", NOW.plusSeconds(60));

        MySummary summary = adapter.count(OWNER_ID, NOW);

        assertThat(summary.treatmentRecordCount()).isEqualTo(3);
        assertThat(summary.sharedRecordCount()).isEqualTo(1);
    }

    /** 후보 스타일만 담은 링크는 시술 기록 카운트에 얹히지 않는다. */
    @Test
    void ignoresSharesThatCarryNoTreatmentRecord() {
        UUID shareId = insertShare(OWNER_ID, null, "ACTIVE", NOW.plusSeconds(60));
        jdbcTemplate.update(
                "INSERT INTO share_saved_styles (share_id, saved_style_id) VALUES (?, ?)",
                shareId, insertSavedStyle(OWNER_ID));

        MySummary summary = adapter.count(OWNER_ID, NOW);

        assertThat(summary.sharedRecordCount()).isZero();
        assertThat(summary.savedStyleCount()).isEqualTo(1);
    }

    /** 한 기록에 성공 분석이 여러 건 쌓여도 "분석한 기록" 은 하나다. */
    @Test
    void countsAnAnalyzedRecordOnceEvenWithSeveralResults() {
        UUID recordId = insertRecord(OWNER_ID);
        insertAnalysisResult(OWNER_ID, recordId);
        insertAnalysisResult(OWNER_ID, recordId);
        insertRecord(OWNER_ID);

        MySummary summary = adapter.count(OWNER_ID, NOW);

        assertThat(summary.treatmentRecordCount()).isEqualTo(2);
        assertThat(summary.analyzedRecordCount()).isEqualTo(1);
    }

    @Test
    void neverCountsAnotherUsersData() {
        UUID otherRecord = insertRecord(OTHER_USER_ID);
        insertAnalysisResult(OTHER_USER_ID, otherRecord);
        insertSavedStyle(OTHER_USER_ID);
        insertShare(OTHER_USER_ID, otherRecord, "ACTIVE", NOW.plusSeconds(60));

        assertThat(adapter.count(OWNER_ID, NOW)).isEqualTo(new MySummary(0, 0, 0, 0));
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

    private UUID insertSavedStyle(UUID ownerId) {
        UUID savedStyleId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO saved_styles (saved_style_id, user_id, style_name)
                VALUES (?, ?, '레이어드 커트')
                """, savedStyleId, ownerId);
        return savedStyleId;
    }

    private UUID insertShare(UUID ownerId, UUID recordId, String status, Instant expiresAt) {
        UUID shareId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO shares (share_id, user_id, token_hash, status, expires_at)
                VALUES (?, ?, ?, ?, ?)
                """, shareId, ownerId, UUID.randomUUID().toString().replace("-", ""),
                status, Timestamp.from(expiresAt));
        if (recordId != null) {
            jdbcTemplate.update(
                    "INSERT INTO share_records (share_id, record_id) VALUES (?, ?)",
                    shareId, recordId);
        }
        return shareId;
    }

    private void insertAnalysisResult(UUID ownerId, UUID recordId) {
        UUID jobId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO analysis_jobs (job_id, user_id, record_id, status)
                VALUES (?, ?, ?, 'SUCCEEDED')
                """, jobId, ownerId, recordId);
        jdbcTemplate.update("""
                INSERT INTO analysis_results (
                    analysis_id, job_id, user_id, record_id,
                    color_uniformity_score, color_uniformity_grade,
                    shape_symmetry_score, shape_symmetry_grade,
                    volume_balance_score, volume_balance_grade,
                    roughness_score, roughness_grade,
                    confidence_score, confidence_grade,
                    model_version, analyzed_at
                ) VALUES (?, ?, ?, ?, 82.4, 'HIGH', 76.2, 'MID', 70.0, 'MID',
                          65.5, 'MID', 90.0, 'HIGH', 'v1', ?)
                """, UUID.randomUUID(), jobId, ownerId, recordId, Timestamp.from(NOW));
    }
}
