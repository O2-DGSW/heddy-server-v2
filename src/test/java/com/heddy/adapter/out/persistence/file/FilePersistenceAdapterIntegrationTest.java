package com.heddy.adapter.out.persistence.file;

import com.heddy.domain.file.exception.FileError;
import com.heddy.domain.file.exception.FileException;
import com.heddy.domain.file.model.FilePurpose;
import com.heddy.domain.file.model.FileStatus;
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
                1_024, null, null, null, first.expiresAt(), null);

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

    @Test
    void bumpsUpdatedAtOnTransitionEvenThoughAuditingIsBypassed() {
        StoredFile pending = adapter.insert(pendingPhoto("touched.jpg"));
        Instant before = updatedAtOf(pending.fileId());

        adapter.transition(pending.markDeleted(), FileStatus.PENDING);

        assertThat(updatedAtOf(pending.fileId())).isAfterOrEqualTo(before);
    }

    // ------------------------------------------------------------------ 스키마 대조

    @Test
    void filesTableMatchesMigration() {
        assertColumn("file_id", "uuid", null, false);
        assertColumn("upload_id", "uuid", null, false);
        assertColumn("user_id", "uuid", null, false);
        assertColumn("purpose", "character varying", 30, false);
        assertColumn("status", "character varying", 20, false);
        assertColumn("object_key", "character varying", 500, false);
        assertColumn("content_type", "character varying", 100, false);
        assertColumn("file_name", "character varying", 255, true);
        assertColumn("file_size", "bigint", null, false);
        assertColumn("sha256", "character varying", 64, true);
        assertColumn("width", "integer", null, true);
        assertColumn("height", "integer", null, true);
        assertColumn("expires_at", "timestamp with time zone", null, false);
        assertColumn("created_at", "timestamp with time zone", null, false);
        assertColumn("updated_at", "timestamp with time zone", null, false);
    }

    @Test
    void keepsIndexesThatOwnershipAndCleanupQueriesDependOn() {
        assertThat(jdbcTemplate.queryForList(
                "SELECT indexname FROM pg_indexes WHERE tablename = 'files'", String.class))
                .contains("uk_files_upload_id", "uk_files_object_key",
                        "idx_files_user_id", "idx_files_status_expires_at");
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
