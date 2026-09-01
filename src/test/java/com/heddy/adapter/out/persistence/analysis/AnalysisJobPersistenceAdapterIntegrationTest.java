package com.heddy.adapter.out.persistence.analysis;

import com.heddy.domain.analysis.model.AnalysisJob;
import com.heddy.domain.analysis.model.AnalysisJobStatus;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AnalysisJobPersistenceAdapterIntegrationTest extends PostgresIntegrationTest {

    private static final UUID USER_ID = UUID.fromString("84000000-0000-4000-8000-000000000001");
    private static final UUID OTHER_USER_ID = UUID.fromString("84000000-0000-4000-8000-000000000002");
    private static final Instant NOW = Instant.parse("2026-08-30T10:00:00Z");

    @Autowired AnalysisJobPersistenceAdapter adapter;
    @Autowired JdbcTemplate jdbcTemplate;
    @PersistenceContext EntityManager entityManager;

    private UUID recordId;
    private UUID photoId;

    @BeforeEach
    void setUpOwnerAndPhoto() {
        insertUser(USER_ID, "analysis-owner@example.com");
        insertUser(OTHER_USER_ID, "analysis-other@example.com");
        recordId = insertRecord(USER_ID);
        photoId = insertPhoto(recordId);
    }

    @Test
    void savesJobAndReadsItBackWithEveryField() {
        AnalysisJob saved = adapter.insert(AnalysisJob.create(USER_ID, recordId, photoId, NOW));

        AnalysisJob found = adapter.findByIdAndUserId(saved.jobId(), USER_ID).orElseThrow();
        assertThat(found.userId()).isEqualTo(USER_ID);
        assertThat(found.recordId()).isEqualTo(recordId);
        assertThat(found.photoId()).isEqualTo(photoId);
        assertThat(found.status()).isEqualTo(AnalysisJobStatus.PENDING);
        assertThat(found.attemptCount()).isEqualTo(1);
        assertThat(found.progress()).isZero();
        assertThat(found.createdAt()).isNotNull();
    }

    @Test
    void writesBackStateTransitions() {
        AnalysisJob saved = adapter.insert(AnalysisJob.create(USER_ID, recordId, photoId, NOW));

        adapter.update(saved.start(NOW).progressTo(60));
        AnalysisJob processing = adapter.findByIdAndUserId(saved.jobId(), USER_ID).orElseThrow();
        assertThat(processing.status()).isEqualTo(AnalysisJobStatus.PROCESSING);
        assertThat(processing.progress()).isEqualTo(60);
        assertThat(processing.startedAt()).isEqualTo(NOW);

        adapter.update(processing.markUnavailable("HAIR_NOT_DETECTED", "머리를 찾지 못했습니다", NOW));
        AnalysisJob unavailable = adapter.findByIdAndUserId(saved.jobId(), USER_ID).orElseThrow();
        assertThat(unavailable.status()).isEqualTo(AnalysisJobStatus.UNAVAILABLE);
        assertThat(unavailable.failureCode()).isEqualTo("HAIR_NOT_DETECTED");
        assertThat(unavailable.failureMessage()).isEqualTo("머리를 찾지 못했습니다");
    }

    /** 남의 작업은 없는 작업과 같다. 소유자 조건이 조회에 함께 실려야 한다. */
    @Test
    void hidesJobsOwnedBySomeoneElse() {
        AnalysisJob saved = adapter.insert(AnalysisJob.create(USER_ID, recordId, photoId, NOW));

        assertThat(adapter.findByIdAndUserId(saved.jobId(), OTHER_USER_ID)).isEmpty();
    }

    /**
     * 같은 사진에 진행 중인 작업이 둘이면 콜백이 어느 작업의 결과인지 갈리지 않는다.
     * 애플리케이션 검사만으로는 동시 요청을 막을 수 없어 DB 가 막는지 확인한다.
     */
    @Test
    void refusesASecondInProgressJobForTheSamePhoto() {
        adapter.insert(AnalysisJob.create(USER_ID, recordId, photoId, NOW));

        assertThatThrownBy(() -> adapter.insert(
                AnalysisJob.create(USER_ID, recordId, photoId, NOW.plusSeconds(1))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /** 끝난 작업은 이력으로 남아야 하므로 같은 사진을 다시 분석할 수 있다. */
    @Test
    void allowsANewJobOnceThePreviousOneHasFinished() {
        AnalysisJob first = adapter.insert(AnalysisJob.create(USER_ID, recordId, photoId, NOW));
        adapter.update(first.start(NOW).fail("AI_TIMEOUT", "시간 초과", NOW));

        AnalysisJob retried = adapter.insert(
                first.start(NOW).fail("AI_TIMEOUT", "시간 초과", NOW).retry(NOW.plusSeconds(60)));

        assertThat(retried.attemptCount()).isEqualTo(2);
        assertThat(adapter.findInProgressByPhotoId(photoId).orElseThrow().jobId())
                .isEqualTo(retried.jobId());
    }

    @Test
    void findsTheMostRecentJobOfARecord() {
        adapter.insert(AnalysisJob.create(USER_ID, recordId, photoId, NOW));
        UUID otherPhotoId = insertPhoto(recordId);
        AnalysisJob newer = adapter.insert(
                AnalysisJob.create(USER_ID, recordId, otherPhotoId, NOW.plusSeconds(60)));

        assertThat(adapter.findLatestByRecordId(recordId).orElseThrow().jobId())
                .isEqualTo(newer.jobId());
    }

    @Test
    void findsNothingForARecordWithoutJobs() {
        assertThat(adapter.findLatestByRecordId(insertRecord(USER_ID))).isEmpty();
        assertThat(adapter.findInProgressByPhotoId(photoId)).isEmpty();
    }

    /** 사진이 지워져도 분석 이력은 남고 대상만 비워진다. CASCADE 였다면 행이 사라진다. */
    @Test
    void keepsTheJobWhenItsSubjectPhotoIsDeleted() {
        AnalysisJob saved = adapter.insert(AnalysisJob.create(USER_ID, recordId, photoId, NOW));
        adapter.update(saved.start(NOW).succeed(NOW));

        jdbcTemplate.update("DELETE FROM treatment_record_photos WHERE photo_id = ?", photoId);
        // DB 가 비운 photo_id 를 보려면 영속성 컨텍스트가 들고 있는 인스턴스를 버려야 한다.
        entityManager.clear();

        AnalysisJob found = adapter.findByIdAndUserId(saved.jobId(), USER_ID).orElseThrow();
        assertThat(found.photoId()).isNull();
        assertThat(found.status()).isEqualTo(AnalysisJobStatus.SUCCEEDED);
    }

    /** 반대로 기록이 사라지면 분석은 가리킬 대상 자체가 없다. */
    @Test
    void removesJobsWhenTheirRecordIsDeleted() {
        AnalysisJob saved = adapter.insert(AnalysisJob.create(USER_ID, recordId, photoId, NOW));

        jdbcTemplate.update("DELETE FROM treatment_record_photos WHERE record_id = ?", recordId);
        jdbcTemplate.update("DELETE FROM treatment_records WHERE record_id = ?", recordId);
        entityManager.clear();

        assertThat(adapter.findByIdAndUserId(saved.jobId(), USER_ID)).isEmpty();
    }

    /**
     * 목록 배지가 쓰는 배치 조회. 기록마다 최신 한 건만 담기고, 분석을 요청한 적 없는 기록은
     * 아예 담기지 않는다 — 호출부가 그걸 null 로 읽는다.
     */
    @Test
    void reportsTheLatestStatusOfEachRecordInOneQuery() {
        AnalysisJob older = adapter.insert(AnalysisJob.create(USER_ID, recordId, photoId, NOW));
        adapter.update(older.start(NOW).fail("AI_TIMEOUT", "시간 초과", NOW));
        UUID newerPhotoId = insertPhoto(recordId);
        AnalysisJob newer = adapter.insert(
                AnalysisJob.create(USER_ID, recordId, newerPhotoId, NOW.plusSeconds(60)));
        adapter.update(newer.start(NOW).succeed(NOW));

        UUID otherRecordId = insertRecord(USER_ID);
        adapter.insert(AnalysisJob.create(USER_ID, otherRecordId, insertPhoto(otherRecordId), NOW));

        UUID neverAnalysedId = insertRecord(USER_ID);

        Map<UUID, AnalysisJobStatus> statuses = adapter.findLatestStatuses(
                List.of(recordId, otherRecordId, neverAnalysedId));

        assertThat(statuses).containsOnly(
                entry(recordId, AnalysisJobStatus.SUCCEEDED),
                entry(otherRecordId, AnalysisJobStatus.PENDING));
    }

    /** 빈 IN 절은 방언에 따라 문법 오류가 된다. 질의 없이 답이 나와야 한다. */
    @Test
    void answersLatestStatusesWithoutQueryingForNoRecords() {
        assertThat(adapter.findLatestStatuses(List.of())).isEmpty();
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
                    content_type, file_size, expires_at
                ) VALUES (?, ?, ?, 'TREATMENT_PHOTO', 'READY', ?, 'image/jpeg', 1024,
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
