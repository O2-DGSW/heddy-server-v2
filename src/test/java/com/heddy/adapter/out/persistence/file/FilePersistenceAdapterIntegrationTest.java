package com.heddy.adapter.out.persistence.file;

import com.heddy.domain.file.exception.FileError;
import com.heddy.domain.file.exception.FileException;
import com.heddy.domain.file.model.FilePurpose;
import com.heddy.domain.file.model.FileStatus;
import com.heddy.domain.file.model.StorageObject;
import com.heddy.domain.file.model.StoredFile;
import com.heddy.domain.file.model.VerifiedContent;
import com.heddy.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.List;
import java.sql.Timestamp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class FilePersistenceAdapterIntegrationTest extends PostgresIntegrationTest {

    private static final UUID USER_ID = UUID.fromString("80000000-0000-4000-8000-000000000001");
    /** presign 으로 선언된 해시. READY 전이로 실측(SHA256)으로 대체되기 전의 값이다. */
    private static final String DECLARED_SHA256 = "b".repeat(64);
    private static final String SHA256 = "c".repeat(64);

    @Autowired FilePersistenceAdapter adapter;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpOwner() {
        insertUser(USER_ID, "file-owner@example.com");
    }

    // ------------------------------------------------------------------ 왕복

    @Test
    void savesPendingFileAndReadsItBack() {
        StoredFile saved = adapter.insert(pendingPhoto("photo-1.jpg"));

        StoredFile found = adapter.findById(saved.fileId()).orElseThrow();

        assertThat(found.uploadId()).isEqualTo(saved.uploadId());
        assertThat(found.userId()).isEqualTo(USER_ID);
        assertThat(found.status()).isEqualTo(FileStatus.PENDING);
        assertThat(found.fileName()).isEqualTo("photo-1.jpg");
        assertThat(found.fileSize()).isEqualTo(1_024);
        // PENDING 행의 해시는 선언값이다. 실측으로 대체되는 시점은 내용 검증이다.
        assertThat(found.sha256()).isEqualTo(DECLARED_SHA256);
        assertThat(found.width()).isNull();
        assertThat(found.createdAt()).isNotNull();
    }

    @Test
    void findsFilesByUploadSessionId() {
        StoredFile saved = adapter.insert(pendingPhoto("by-upload-id.jpg"));

        StoredFile found = adapter.findByUploadId(saved.uploadId()).orElseThrow();

        assertThat(found.fileId()).isEqualTo(saved.fileId());
        assertThat(found.uploadId()).isEqualTo(saved.uploadId());
    }

    @Test
    void reportsEmptyForUnknownUploadSessionIds() {
        assertThat(adapter.findByUploadId(UUID.randomUUID())).isEmpty();
    }

    @Test
    void storesVerifiedContentFactsOnTransitionToReady() {
        StoredFile pending = adapter.insert(pendingPhoto("photo-2.jpg"));

        adapter.transition(
                pending.markReady(new VerifiedContent("image/png", 4_096, SHA256, 800, 600)),
                FileStatus.PENDING);

        StoredFile ready = adapter.findById(pending.fileId()).orElseThrow();
        assertThat(rowCountFor(pending.fileId())).isEqualTo(1);
        assertThat(ready.status()).isEqualTo(FileStatus.READY);
        assertThat(ready.contentType()).isEqualTo("image/png");
        assertThat(ready.fileSize()).isEqualTo(4_096);
        assertThat(ready.sha256()).isEqualTo(SHA256);
        assertThat(ready.width()).isEqualTo(800);
        assertThat(ready.height()).isEqualTo(600);
    }

    @Test
    void rejectsDuplicateObjectKey() {
        adapter.insert(pendingPhoto("same-key.jpg"));

        assertThatThrownBy(() -> adapter.insert(pendingPhoto("same-key.jpg")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsDuplicateUploadId() {
        StoredFile first = adapter.insert(pendingPhoto("upload-1.jpg"));
        StoredFile clashing = new StoredFile(
                UUID.randomUUID(), first.uploadId(), USER_ID, FilePurpose.TREATMENT_PHOTO,
                FileStatus.PENDING, "TREATMENT_PHOTO/other.jpg", "image/jpeg", "other.jpg",
                1_024, null, null, null, first.expiresAt(), null, null);

        assertThatThrownBy(() -> adapter.insert(clashing))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ------------------------------------------------------------------ 상태 전이 경합

    /**
     * complete 와 delete 가 같은 PENDING 파일을 읽고 각각 READY·DELETED 스냅샷을 만든 뒤,
     * delete 가 먼저 저장되고 complete 가 늦게 도착하는 순서를 재현한다.
     *
     * <p>스레드를 띄우지 않고 순서를 고정한 이유는, 경쟁을 진짜로 돌리면 어느 쪽이 이길지에 따라
     * 결과가 갈려 테스트가 간헐적으로 통과하기 때문이다. 막아야 하는 것은 "늦게 온 쓰기"이므로
     * 그 순서를 직접 만든다.
     */
    @Test
    void refusesLateWriteThatWouldResurrectDeletedFile() {
        StoredFile pending = adapter.insert(pendingPhoto("racing.jpg"));
        StoredFile deleted = pending.markDeleted();
        StoredFile ready = pending.markReady(new VerifiedContent("image/jpeg", 1_024, SHA256, 800, 600));

        adapter.transition(deleted, FileStatus.PENDING);

        assertThatThrownBy(() -> adapter.transition(ready, FileStatus.PENDING))
                .isInstanceOf(FileException.class)
                .extracting(exception -> ((FileException) exception).error())
                .isEqualTo(FileError.CONCURRENT_MODIFICATION);
        assertThat(adapter.findById(pending.fileId()).orElseThrow().status())
                .isEqualTo(FileStatus.DELETED);
    }

    @Test
    void refusesSecondCompleteAfterFirstOneWon() {
        StoredFile pending = adapter.insert(pendingPhoto("double-complete.jpg"));
        StoredFile ready = pending.markReady(new VerifiedContent("image/jpeg", 1_024, SHA256, 800, 600));
        adapter.transition(ready, FileStatus.PENDING);

        assertThatThrownBy(() -> adapter.transition(ready, FileStatus.PENDING))
                .isInstanceOf(FileException.class);
    }

    // ------------------------------------------------------------------ 취소 객체 회수

    /**
     * 취소가 지운 객체는 발급된 presigned PUT URL 이 살아 있는 동안 되살아날 수 있어 최종 회수가
     * 아니다. 그래서 만료된 DELETED 행은 회수 표시가 채워질 때까지 계속 대상으로 남는다.
     */
    @Test
    void listsExpiredCancelledSessionsAsReclaimTargetsUntilTheyAreMarked() {
        StoredFile cancelled = adapter.insert(expiredPendingPhoto("reclaim-me.jpg"));
        adapter.transition(cancelled.markDeleted(), FileStatus.PENDING);

        assertThat(adapter.findReclaimTargets(Instant.now(), 10))
                .extracting(StoredFile::fileId)
                .contains(cancelled.fileId());

        adapter.markReclaimed(cancelled.fileId(), Instant.now());

        assertThat(adapter.findReclaimTargets(Instant.now(), 10))
                .extracting(StoredFile::fileId)
                .doesNotContain(cancelled.fileId());
    }

    /** 아직 만료되지 않은 취소 세션은 대상이 아니다. URL 이 살아 있는 동안은 다시 지워도 소용없다. */
    @Test
    void skipsCancelledSessionsWhoseUploadUrlIsStillValid() {
        StoredFile cancelled = adapter.insert(pendingPhoto("still-valid.jpg"));
        adapter.transition(cancelled.markDeleted(), FileStatus.PENDING);

        assertThat(adapter.findReclaimTargets(Instant.now(), 10))
                .extracting(StoredFile::fileId)
                .doesNotContain(cancelled.fileId());
    }

    /** 취소되지 않은 세션은 상태와 무관하게 이 경로가 건드리지 않는다. */
    @Test
    void skipsSessionsThatWereNeverCancelled() {
        StoredFile pending = adapter.insert(expiredPendingPhoto("expired-pending.jpg"));
        StoredFile readySource = adapter.insert(expiredPendingPhoto("expired-ready.jpg"));
        adapter.transition(
                readySource.markReady(new VerifiedContent("image/jpeg", 1_024, SHA256, 800, 600)),
                FileStatus.PENDING);

        assertThat(adapter.findReclaimTargets(Instant.now(), 10))
                .extracting(StoredFile::fileId)
                .doesNotContain(pending.fileId(), readySource.fileId());
    }

    @Test
    void honoursTheReclaimBatchLimit() {
        adapter.transition(
                adapter.insert(expiredPendingPhoto("batch-1.jpg")).markDeleted(), FileStatus.PENDING);
        adapter.transition(
                adapter.insert(expiredPendingPhoto("batch-2.jpg")).markDeleted(), FileStatus.PENDING);

        assertThat(adapter.findReclaimTargets(Instant.now(), 1)).hasSize(1);
    }

    @Test
    void bumpsUpdatedAtOnTransitionEvenThoughAuditingIsBypassed() {
        StoredFile pending = adapter.insert(pendingPhoto("touched.jpg"));
        Instant before = updatedAtOf(pending.fileId());

        adapter.transition(pending.markDeleted(), FileStatus.PENDING);

        assertThat(updatedAtOf(pending.fileId())).isAfterOrEqualTo(before);
    }

    @Test
    void findsExpiredPendingUnlinkedReadyAndExpiredDeletedCleanupCandidates() {
        Instant now = Instant.now();
        StoredFile expiredPending = adapter.insert(new StoredFile(
                UUID.randomUUID(), UUID.randomUUID(), USER_ID, FilePurpose.TREATMENT_PHOTO,
                FileStatus.PENDING, "TREATMENT_PHOTO/expired.jpg", "image/jpeg", "expired.jpg",
                100, null, null, null, now.minusSeconds(120), null, null));
        StoredFile freshPending = adapter.insert(pendingPhoto("fresh.jpg"));
        StoredFile orphanReady = adapter.insert(pendingPhoto("orphan.jpg"));
        orphanReady = adapter.transition(
                orphanReady.markReady(new com.heddy.domain.file.model.StorageObject("image/jpeg", 1_024)),
                FileStatus.PENDING);
        jdbcTemplate.update("UPDATE files SET created_at = ? WHERE file_id = ?",
                Timestamp.from(now.minusSeconds(172_800)), orphanReady.fileId());
        StoredFile deleted = adapter.insert(StoredFile.pending(
                USER_ID, FilePurpose.TREATMENT_PHOTO,
                "TREATMENT_PHOTO/" + USER_ID + "/deleted.jpg",
                "image/jpeg", "deleted.jpg", 1_024, DECLARED_SHA256,
                now.minusSeconds(120)));
        adapter.transition(deleted.markDeleted(), FileStatus.PENDING);
        StoredFile freshDeleted = adapter.insert(pendingPhoto("fresh-deleted.jpg"));
        adapter.transition(freshDeleted.markDeleted(), FileStatus.PENDING);

        List<StoredFile> candidates = adapter.findCleanupCandidates(
                now.minusSeconds(60), now.minusSeconds(86_400), 100);

        assertThat(candidates).extracting(StoredFile::fileId)
                .contains(expiredPending.fileId(), orphanReady.fileId(), deleted.fileId())
                .doesNotContain(freshPending.fileId(), freshDeleted.fileId());
    }

    /**
     * READY 고아 판정은 파일을 직접 참조하는 모든 테이블을 알아야 한다. 참조 누락은 DB 행은
     * READY 로 남긴 채 스토리지 객체만 먼저 지우므로, API 가 발급한 정상 서명 URL이
     * {@code NoSuchKey} 로 깨지는 운영 장애가 된다.
     */
    @Test
    void excludesEveryReferencedReadyFileFromCleanupCandidates() {
        Instant now = Instant.now();
        StoredFile treatmentPhoto = oldReadyUserFile(
                "referenced-treatment.jpg", FilePurpose.TREATMENT_PHOTO, "image/jpeg", now);
        StoredFile analysisOverlay = oldReadySystemFile(
                "referenced-overlay.png", FilePurpose.ANALYSIS_OVERLAY_INTERNAL, "image/png", now);
        StoredFile thumbnail = oldReadySystemFile(
                "referenced-thumbnail.jpg", FilePurpose.HAIRSTYLE_THUMBNAIL, "image/jpeg", now);
        StoredFile arBase = oldReadySystemFile(
                "referenced-ar-base.png", FilePurpose.HAIRSTYLE_AR_BASE, "image/png", now);
        StoredFile arMask = oldReadySystemFile(
                "referenced-ar-mask.png", FilePurpose.HAIRSTYLE_AR_MASK, "image/png", now);
        StoredFile savedCapture = oldReadyUserFile(
                "referenced-capture.jpg", FilePurpose.AR_CAPTURE, "image/jpeg", now);
        StoredFile orphan = oldReadyUserFile(
                "unreferenced.jpg", FilePurpose.TREATMENT_PHOTO, "image/jpeg", now);

        UUID recordId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO treatment_records (record_id, user_id, service_types, performed_at)
                VALUES (?, ?, '[]'::jsonb, now())
                """, recordId, USER_ID);
        jdbcTemplate.update("""
                INSERT INTO treatment_record_photos
                    (photo_id, record_id, file_id, image_type, sort_order)
                VALUES (?, ?, ?, 'BEFORE', 0)
                """, UUID.randomUUID(), recordId, treatmentPhoto.fileId());

        UUID jobId = UUID.randomUUID();
        UUID analysisId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO analysis_jobs
                    (job_id, user_id, record_id, status, progress, attempt_count)
                VALUES (?, ?, ?, 'SUCCEEDED', 100, 1)
                """, jobId, USER_ID, recordId);
        jdbcTemplate.update("""
                INSERT INTO analysis_results (
                    analysis_id, job_id, user_id, record_id,
                    color_uniformity_score, color_uniformity_grade,
                    shape_symmetry_score, shape_symmetry_grade,
                    volume_balance_score, volume_balance_grade,
                    roughness_score, roughness_grade,
                    confidence_score, confidence_grade, model_version, analyzed_at
                ) VALUES (
                    ?, ?, ?, ?, 80, 'HIGH', 80, 'HIGH', 80, 'HIGH',
                    80, 'HIGH', 80, 'HIGH', 'test', now()
                )
                """, analysisId, jobId, USER_ID, recordId);
        jdbcTemplate.update("""
                INSERT INTO analysis_overlays (overlay_id, analysis_id, overlay_type, file_id)
                VALUES (?, ?, 'HEATMAP', ?)
                """, UUID.randomUUID(), analysisId, analysisOverlay.fileId());

        UUID hairstyleId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO hairstyle_assets (
                    hairstyle_id, style_name, category, thumbnail_file_id,
                    base_file_id, mask_file_id, asset_version
                ) VALUES (?, '정리 참조 테스트', 'TEST', ?, ?, ?, 'test')
                """, hairstyleId, thumbnail.fileId(), arBase.fileId(), arMask.fileId());
        jdbcTemplate.update("""
                INSERT INTO saved_styles (saved_style_id, user_id, style_name, capture_id)
                VALUES (?, ?, '저장 후보 정리 테스트', ?)
                """, UUID.randomUUID(), USER_ID, savedCapture.fileId());

        List<StoredFile> candidates = adapter.findCleanupCandidates(
                now.minusSeconds(60), now.minusSeconds(86_400), 100);

        assertThat(candidates).extracting(StoredFile::fileId)
                .contains(orphan.fileId())
                .doesNotContain(
                        treatmentPhoto.fileId(), analysisOverlay.fileId(), thumbnail.fileId(),
                        arBase.fileId(), arMask.fileId(), savedCapture.fileId());
    }

    // ------------------------------------------------------------------ 스키마 대조

    @Test
    void filesTableMatchesMigration() {
        assertColumn("file_id", "uuid", null, false);
        assertColumn("upload_id", "uuid", null, false);
        assertColumn("user_id", "uuid", null, true);
        assertColumn("owner_type", "character varying", 10, false);
        assertColumn("purpose", "character varying", 30, false);
        assertColumn("status", "character varying", 20, false);
        assertColumn("object_key", "character varying", 500, false);
        assertColumn("content_type", "character varying", 100, false);
        assertColumn("file_name", "character varying", 255, false);
        assertColumn("file_size", "bigint", null, false);
        assertColumn("sha256", "character varying", 64, true);
        assertColumn("width", "integer", null, true);
        assertColumn("height", "integer", null, true);
        assertColumn("expires_at", "timestamp with time zone", null, false);
        assertColumn("reclaimed_at", "timestamp with time zone", null, true);
        assertColumn("created_at", "timestamp with time zone", null, false);
        assertColumn("updated_at", "timestamp with time zone", null, false);
    }

    @Test
    void keepsIndexesThatOwnershipAndCleanupQueriesDependOn() {
        assertThat(jdbcTemplate.queryForList(
                "SELECT indexname FROM pg_indexes WHERE tablename = 'files'", String.class))
                .contains("uk_files_upload_id", "uk_files_object_key",
                        "idx_files_user_id", "idx_files_status_expires_at",
                        "idx_files_reclaim_targets");

        jdbcTemplate.execute("SET LOCAL enable_seqscan = off");
        String plan = String.join("\n", jdbcTemplate.queryForList("""
                EXPLAIN SELECT file_id FROM files
                WHERE status = 'PENDING' AND expires_at <= now()
                """, String.class));
        assertThat(plan).contains("idx_files_status_expires_at");
    }

    /**
     * 도메인이 fileName 을 필수로 요구하므로(StoredFile) 값이 없는 행은 읽는 순간 터진다.
     * 실제로 값이 없는 행이 남아 있어 시술기록 목록 조회가 통째로 500 이 됐다(#122).
     *
     * <p>애플리케이션 경로로는 이런 행을 만들 수 없어 이 테스트는 SQL 로 직접 넣는다.
     * 도메인을 거치면 값이 없다는 사실이 저장 계층에 닿기 전에 걸리기 때문이다.
     */
    @Test
    void refusesFileRowsWithoutFileName() {
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO files (
                    file_id, upload_id, user_id, owner_type, purpose, status,
                    object_key, content_type, file_name, file_size, expires_at
                ) VALUES (?, ?, ?, 'USER', 'TREATMENT_PHOTO', 'PENDING',
                    'TREATMENT_PHOTO/nameless.jpg', 'image/jpeg', NULL, 1024, now())
                """, UUID.randomUUID(), UUID.randomUUID(), USER_ID))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void doesNotCascadeFileRowsWhenOwnerIsDeleted() {
        adapter.insert(pendingPhoto("keeps-object-key.jpg"));

        // CASCADE 였다면 사용자 삭제가 조용히 성공하고 object_key 가 사라져 객체를 회수할 수 없다.
        assertThatThrownBy(() -> jdbcTemplate.update(
                "DELETE FROM users WHERE user_id = ?", USER_ID))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ------------------------------------------------------------------ 헬퍼

    private static StoredFile pendingPhoto(String name) {
        return StoredFile.pending(
                USER_ID, FilePurpose.TREATMENT_PHOTO, "TREATMENT_PHOTO/" + USER_ID + "/" + name,
                "image/jpeg", name, 1_024, DECLARED_SHA256,
                Instant.now().plus(5, ChronoUnit.MINUTES));
    }

    private static StoredFile expiredPendingPhoto(String name) {
        return StoredFile.pending(
                USER_ID, FilePurpose.TREATMENT_PHOTO, "TREATMENT_PHOTO/" + USER_ID + "/" + name,
                "image/jpeg", name, 1_024, DECLARED_SHA256,
                Instant.now().minus(1, ChronoUnit.MINUTES));
    }

    private StoredFile oldReadyUserFile(
            String name, FilePurpose purpose, String contentType, Instant now
    ) {
        StoredFile pending = adapter.insert(StoredFile.pending(
                USER_ID, purpose, purpose + "/" + USER_ID + "/" + name,
                contentType, name, 1_024, DECLARED_SHA256,
                now.plus(5, ChronoUnit.MINUTES)));
        StoredFile ready = adapter.transition(
                pending.markReady(new StorageObject(contentType, 1_024)), FileStatus.PENDING);
        makeOld(ready.fileId(), now);
        return ready;
    }

    private StoredFile oldReadySystemFile(
            String name, FilePurpose purpose, String contentType, Instant now
    ) {
        StoredFile pending = adapter.insert(StoredFile.pendingSystem(
                purpose, purpose + "/system/" + name, contentType, name, 1_024,
                DECLARED_SHA256, now.plus(5, ChronoUnit.MINUTES)));
        StoredFile ready = adapter.transition(
                pending.markReady(new StorageObject(contentType, 1_024)), FileStatus.PENDING);
        makeOld(ready.fileId(), now);
        return ready;
    }

    private void makeOld(UUID fileId, Instant now) {
        jdbcTemplate.update("UPDATE files SET created_at = ? WHERE file_id = ?",
                Timestamp.from(now.minusSeconds(172_800)), fileId);
    }

    private void insertUser(UUID userId, String email) {
        jdbcTemplate.update("""
                INSERT INTO users (
                    user_id, email, password_hash, auth_provider, status, login_fail_count
                ) VALUES (?, ?, ?, 'EMAIL', 'ACTIVE', 0)
                """, userId, email, "hash");
    }

    private int rowCountFor(UUID fileId) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM files WHERE file_id = ?", Integer.class, fileId);
    }

    private Instant updatedAtOf(UUID fileId) {
        return jdbcTemplate.queryForObject(
                "SELECT updated_at FROM files WHERE file_id = ?", Instant.class, fileId);
    }

    private void assertColumn(String name, String dataType, Integer length, boolean nullable) {
        var column = jdbcTemplate.queryForMap("""
                SELECT data_type, character_maximum_length, is_nullable
                FROM information_schema.columns
                WHERE table_name = 'files' AND column_name = ?
                """, name);

        assertThat(column.get("data_type")).as("%s 타입", name).isEqualTo(dataType);
        assertThat(column.get("character_maximum_length")).as("%s 길이", name).isEqualTo(length);
        assertThat(column.get("is_nullable")).as("%s nullable", name)
                .isEqualTo(nullable ? "YES" : "NO");
    }
}
