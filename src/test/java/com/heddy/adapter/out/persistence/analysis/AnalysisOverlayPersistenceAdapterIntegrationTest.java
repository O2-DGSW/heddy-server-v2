package com.heddy.adapter.out.persistence.analysis;

import com.heddy.domain.analysis.model.AnalysisJob;
import com.heddy.domain.analysis.model.AnalysisJobStatus;
import com.heddy.domain.analysis.model.AnalysisOverlay;
import com.heddy.domain.analysis.model.AnalysisResult;
import com.heddy.domain.analysis.model.ConfidenceGrade;
import com.heddy.domain.analysis.model.MetricScore;
import com.heddy.domain.analysis.model.MetricType;
import com.heddy.domain.analysis.model.OverlayType;
import com.heddy.domain.analysis.port.out.AnalysisStalenessPort;
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

import java.sql.Timestamp;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AnalysisOverlayPersistenceAdapterIntegrationTest extends PostgresIntegrationTest {

    private static final UUID USER_ID = UUID.fromString("86000000-0000-4000-8000-000000000001");
    private static final Instant NOW = Instant.parse("2026-08-30T10:00:00Z");

    @Autowired AnalysisOverlayPersistenceAdapter adapter;
    @Autowired AnalysisResultPersistenceAdapter resultAdapter;
    @Autowired AnalysisJobPersistenceAdapter jobAdapter;
    @Autowired AnalysisStalenessPort stalenessPort;
    @Autowired JdbcTemplate jdbcTemplate;
    @PersistenceContext EntityManager entityManager;

    private UUID recordId;
    private UUID photoId;

    @BeforeEach
    void setUpOwnerAndPhoto() {
        insertUser(USER_ID, "overlay-owner@example.com");
        recordId = insertRecord(USER_ID);
        photoId = insertPhoto(recordId);
    }

    @Test
    void savesEveryOverlayKindOfAResult() {
        UUID analysisId = savedResult().analysisId();

        adapter.insert(AnalysisOverlay.create(analysisId, OverlayType.HAIR_MASK, newFile(), NOW));
        adapter.insert(
                AnalysisOverlay.create(analysisId, OverlayType.COLOR_DIFFERENCE, newFile(), NOW));
        adapter.insert(
                AnalysisOverlay.create(analysisId, OverlayType.VOLUME_GUIDE, newFile(), NOW));

        assertThat(adapter.findByAnalysisId(analysisId))
                .extracting(AnalysisOverlay::overlayType)
                .containsExactlyInAnyOrder(OverlayType.HAIR_MASK, OverlayType.COLOR_DIFFERENCE,
                        OverlayType.VOLUME_GUIDE);
    }

    /** 콜백이 중복 도착해도 같은 종류가 두 번 쌓이지 않게 DB 가 막는다. */
    @Test
    void refusesASecondOverlayOfTheSameKind() {
        UUID analysisId = savedResult().analysisId();
        adapter.insert(AnalysisOverlay.create(analysisId, OverlayType.HAIR_MASK, newFile(), NOW));

        assertThatThrownBy(() -> adapter.insert(
                AnalysisOverlay.create(analysisId, OverlayType.HAIR_MASK, newFile(), NOW)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /** 결과가 사라지면 그 결과를 겹쳐 보여줄 대상도 사라진다. */
    @Test
    void removesOverlaysWhenTheirResultIsDeleted() {
        AnalysisResult result = savedResult();
        adapter.insert(
                AnalysisOverlay.create(result.analysisId(), OverlayType.HAIR_MASK, newFile(), NOW));

        jdbcTemplate.update("DELETE FROM analysis_results WHERE analysis_id = ?",
                result.analysisId());
        entityManager.clear();

        assertThat(adapter.findByAnalysisId(result.analysisId())).isEmpty();
    }

    @Test
    void findsNothingForAResultWithoutOverlays() {
        assertThat(adapter.findByAnalysisId(savedResult().analysisId())).isEmpty();
    }

    /**
     * 무효화 지점이 실제로 연결됐는지 본다. 구현체가 없으면 no-op 폴백이 조용히 아무 일도 하지
     * 않으므로, 스프링이 실제 서비스를 물렸는지는 상태가 바뀌는 것으로만 확인된다.
     */
    @Test
    void wiresTheStalenessPortToARealImplementation() {
        AnalysisJob job = jobAdapter.insert(AnalysisJob.create(USER_ID, recordId, photoId, NOW));
        jobAdapter.update(job.start(NOW).succeed(NOW));

        stalenessPort.markLatestStale(recordId);
        entityManager.clear();

        assertThat(jobAdapter.findByIdAndUserId(job.jobId(), USER_ID).orElseThrow().status())
                .isEqualTo(AnalysisJobStatus.STALE);
    }

    // ------------------------------------------------------------------ 헬퍼

    private AnalysisResult savedResult() {
        AnalysisJob job = jobAdapter.insert(
                AnalysisJob.create(USER_ID, recordId, insertPhoto(recordId), NOW));
        AnalysisJob succeeded = jobAdapter.update(job.start(NOW).succeed(NOW));
        Map<MetricType, MetricScore> metrics = new EnumMap<>(MetricType.class);
        for (MetricType type : MetricType.values()) {
            metrics.put(type, MetricScore.of("80.00", ConfidenceGrade.HIGH));
        }
        return resultAdapter.insert(AnalysisResult.create(succeeded, metrics,
                MetricScore.of("80.00", ConfidenceGrade.HIGH), "hair-v1.2.0", null, null, NOW));
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

    private UUID newFile() {
        UUID fileId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO files (
                    file_id, upload_id, user_id, purpose, status, object_key,
                    content_type, file_name, file_size, expires_at
                ) VALUES (?, ?, ?, 'ANALYSIS_OVERLAY_INTERNAL', 'READY', ?, 'image/png', 'overlay.png', 2048,
                          now() + interval '5 minutes')
                """, fileId, UUID.randomUUID(), USER_ID, "ANALYSIS_OVERLAY_INTERNAL/" + fileId);
        return fileId;
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
