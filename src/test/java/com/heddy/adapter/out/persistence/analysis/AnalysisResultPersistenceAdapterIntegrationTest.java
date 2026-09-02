package com.heddy.adapter.out.persistence.analysis;

import com.heddy.domain.analysis.model.AnalysisJob;
import com.heddy.domain.analysis.model.AnalysisResult;
import com.heddy.domain.analysis.model.ConfidenceGrade;
import com.heddy.domain.analysis.model.MetricScore;
import com.heddy.domain.analysis.model.MetricType;
import com.heddy.support.PostgresIntegrationTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AnalysisResultPersistenceAdapterIntegrationTest extends PostgresIntegrationTest {

    private static final UUID USER_ID = UUID.fromString("85000000-0000-4000-8000-000000000001");
    private static final UUID OTHER_USER_ID = UUID.fromString("85000000-0000-4000-8000-000000000002");
    private static final Instant NOW = Instant.parse("2026-08-30T10:00:00Z");

    @Autowired AnalysisResultPersistenceAdapter adapter;
    @Autowired AnalysisJobPersistenceAdapter jobAdapter;
    @Autowired JdbcTemplate jdbcTemplate;
    @PersistenceContext EntityManager entityManager;

    private UUID recordId;
    private UUID photoId;

    @BeforeEach
    void setUpOwnerAndPhoto() {
        insertUser(USER_ID, "result-owner@example.com");
        insertUser(OTHER_USER_ID, "result-other@example.com");
        recordId = insertRecord(USER_ID);
        photoId = insertPhoto(recordId);
    }

    @Test
    void savesResultAndReadsEveryMetricBack() {
        AnalysisResult saved = adapter.insert(resultOf(succeededJob(photoId)));

        AnalysisResult found = adapter.findByIdAndUserId(saved.analysisId(), USER_ID).orElseThrow();
        assertThat(found.metric(MetricType.COLOR_UNIFORMITY).score())
                .isEqualByComparingTo(new BigDecimal("82.40"));
        assertThat(found.metric(MetricType.COLOR_UNIFORMITY).grade())
                .isEqualTo(ConfidenceGrade.HIGH);
        assertThat(found.metric(MetricType.ROUGHNESS).score())
                .isEqualByComparingTo(new BigDecimal("34.50"));
        assertThat(found.confidence().grade()).isEqualTo(ConfidenceGrade.MEDIUM);
        assertThat(found.modelVersion()).isEqualTo("hair-v1.2.0");
        assertThat(found.summary()).isEqualTo("사진에서 거칠게 보이는 영역이 감지되었습니다");
        assertThat(found.analyzedAt()).isEqualTo(NOW);
    }

    /** 소수점이 깎이면 비교 분석의 Δ값이 그만큼 어긋난다. */
    @Test
    void storesScoresWithoutRoundingThemToWholeNumbers() {
        AnalysisResult saved = adapter.insert(resultOf(succeededJob(photoId)));
        entityManager.clear();

        assertThat(adapter.findByIdAndUserId(saved.analysisId(), USER_ID).orElseThrow()
                .metric(MetricType.SHAPE_SYMMETRY).score())
                .isEqualByComparingTo(new BigDecimal("76.20"));
    }

    @Test
    void storesEvidenceAsJson() {
        AnalysisJob job = succeededJob(photoId);
        adapter.insert(AnalysisResult.create(job, metrics(),
                MetricScore.of("78.00", ConfidenceGrade.MEDIUM), "hair-v1.2.0", null,
                "{\"regions\": [\"crown\"]}", NOW));
        entityManager.clear();

        assertThat(adapter.findByJobId(job.jobId()).orElseThrow().evidence())
                .contains("crown");
    }

    /** 남의 결과는 없는 결과와 같다. */
    @Test
    void hidesResultsOwnedBySomeoneElse() {
        AnalysisResult saved = adapter.insert(resultOf(succeededJob(photoId)));

        assertThat(adapter.findByIdAndUserId(saved.analysisId(), OTHER_USER_ID)).isEmpty();
    }

    /** 콜백이 중복 도착해도 완료된 결과를 덮어쓰지 않게 DB 가 막는 마지막 방어선이다. */
    @Test
    void refusesASecondResultForTheSameJob() {
        AnalysisJob job = succeededJob(photoId);
        adapter.insert(resultOf(job));

        assertThatThrownBy(() -> adapter.insert(resultOf(job)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findsTheMostRecentResultOfARecord() {
        adapter.insert(resultOf(succeededJob(photoId), NOW));
        AnalysisJob newerJob = succeededJob(insertPhoto(recordId));
        AnalysisResult newer = adapter.insert(resultOf(newerJob, NOW.plusSeconds(600)));

        assertThat(adapter.findLatestByRecordId(recordId).orElseThrow().analysisId())
                .isEqualTo(newer.analysisId());
    }

    /** 사진이 지워져도 결과 이력은 남고 대상만 비워진다. */
    @Test
    void keepsTheResultWhenItsSubjectPhotoIsDeleted() {
        AnalysisResult saved = adapter.insert(resultOf(succeededJob(photoId)));

        jdbcTemplate.update("DELETE FROM treatment_record_photos WHERE photo_id = ?", photoId);
        entityManager.clear();

        assertThat(adapter.findByIdAndUserId(saved.analysisId(), USER_ID).orElseThrow().photoId())
                .isNull();
    }

    @Test
    void findsNothingForARecordWithoutResults() {
        assertThat(adapter.findLatestByRecordId(insertRecord(USER_ID))).isEmpty();
        assertThat(adapter.findByJobId(UUID.randomUUID())).isEmpty();
    }

    // ------------------------------------------------------------------ 헬퍼

    private AnalysisJob succeededJob(UUID subjectPhotoId) {
        AnalysisJob job = jobAdapter.insert(
                AnalysisJob.create(USER_ID, recordId, subjectPhotoId, NOW));
        return jobAdapter.update(job.start(NOW).succeed(NOW));
    }

    private AnalysisResult resultOf(AnalysisJob job) {
        return resultOf(job, NOW);
    }

    private AnalysisResult resultOf(AnalysisJob job, Instant analyzedAt) {
        return AnalysisResult.create(job, metrics(),
                MetricScore.of("78.00", ConfidenceGrade.MEDIUM), "hair-v1.2.0",
                "사진에서 거칠게 보이는 영역이 감지되었습니다", null, analyzedAt);
    }

    private Map<MetricType, MetricScore> metrics() {
        Map<MetricType, MetricScore> metrics = new EnumMap<>(MetricType.class);
        metrics.put(MetricType.COLOR_UNIFORMITY, MetricScore.of("82.40", ConfidenceGrade.HIGH));
        metrics.put(MetricType.SHAPE_SYMMETRY, MetricScore.of("76.20", ConfidenceGrade.MEDIUM));
        metrics.put(MetricType.VOLUME_BALANCE, MetricScore.of("71.00", ConfidenceGrade.MEDIUM));
        metrics.put(MetricType.ROUGHNESS, MetricScore.of("34.50", ConfidenceGrade.HIGH));
        return metrics;
    }

    private void insertUser(UUID userId, String email) {
        jdbcTemplate.update("""
                INSERT INTO users (
                    user_id, email, password_hash, auth_provider, status, login_fail_count
                ) VALUES (?, ?, ?, 'EMAIL', 'ACTIVE', 0)
                """, userId, email, "hash");
    }

    private UUID insertRecord(UUID ownerId) {
        UUID newRecordId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO treatment_records (record_id, user_id, service_types, performed_at)
                VALUES (?, ?, CAST(? AS jsonb), ?)
                """, newRecordId, ownerId, "[\"CUT\"]", Timestamp.from(NOW.minusSeconds(3600)));
        return newRecordId;
    }

    private UUID insertPhoto(UUID ownerRecordId) {
        UUID fileId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO files (
                    file_id, upload_id, user_id, purpose, status, object_key,
                    content_type, file_name, file_size, expires_at
                ) VALUES (?, ?, ?, 'TREATMENT_PHOTO', 'READY', ?, 'image/jpeg', 'photo.jpg', 1024,
                          now() + interval '5 minutes')
                """, fileId, UUID.randomUUID(), USER_ID, "TREATMENT_PHOTO/" + USER_ID + "/" + fileId);
        UUID newPhotoId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO treatment_record_photos (photo_id, record_id, file_id, image_type)
                VALUES (?, ?, ?, 'AFTER')
                """, newPhotoId, ownerRecordId, fileId);
        return newPhotoId;
    }
}
