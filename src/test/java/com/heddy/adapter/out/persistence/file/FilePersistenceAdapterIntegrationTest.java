package com.heddy.adapter.out.persistence.file;

import com.heddy.domain.file.model.FilePurpose;
import com.heddy.domain.file.model.FileStatus;
import com.heddy.domain.file.model.StoredFile;
import com.heddy.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class FilePersistenceAdapterIntegrationTest extends PostgresIntegrationTest {

    private static final UUID OWNER_ID = UUID.fromString("80000000-0000-4000-8000-000000000001");

    @Autowired FilePersistenceAdapter adapter;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpOwner() {
        insertUser(OWNER_ID, "file-owner@example.com");
    }

    // ------------------------------------------------------------------ 왕복

    @Test
    void savesPendingFileAndReadsItBack() {
        StoredFile saved = adapter.save(pendingPhoto(OWNER_ID, "photo-1.jpg"));

        StoredFile found = adapter.findById(saved.fileId()).orElseThrow();

        assertThat(found.ownerId()).isEqualTo(OWNER_ID);
        assertThat(found.purpose()).isEqualTo(FilePurpose.TREATMENT_PHOTO);
        assertThat(found.status()).isEqualTo(FileStatus.PENDING);
        assertThat(found.byteSize()).isEqualTo(1_024);
        assertThat(found.createdAt()).isNotNull();
    }

    @Test
    void updatesExistingRowInsteadOfInsertingWhenStatusChanges() {
        StoredFile pending = adapter.save(pendingPhoto(OWNER_ID, "photo-2.jpg"));

        adapter.save(pending.markReady("image/png", 4_096));

        assertThat(rowCountFor(pending.fileId())).isEqualTo(1);
        StoredFile ready = adapter.findById(pending.fileId()).orElseThrow();
        assertThat(ready.status()).isEqualTo(FileStatus.READY);
        assertThat(ready.contentType()).isEqualTo("image/png");
        assertThat(ready.byteSize()).isEqualTo(4_096);
    }

    @Test
    void rejectsDuplicateObjectKey() {
        adapter.save(pendingPhoto(OWNER_ID, "same-key.jpg"));

        assertThatThrownBy(() -> adapter.save(pendingPhoto(OWNER_ID, "same-key.jpg")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ------------------------------------------------------------------ 스키마 대조

    @Test
    void filesTableMatchesMigration() {
        assertColumn("file_id", "uuid", null, false);
        assertColumn("owner_id", "uuid", null, false);
        assertColumn("purpose", "character varying", 30, false);
        assertColumn("status", "character varying", 10, false);
        assertColumn("object_key", "character varying", 255, false);
        assertColumn("content_type", "character varying", 100, false);
        assertColumn("byte_size", "bigint", null, false);
        assertColumn("created_at", "timestamp with time zone", null, false);
        assertColumn("updated_at", "timestamp with time zone", null, false);
    }

    @Test
    void keepsIndexesThatOwnershipAndCleanupQueriesDependOn() {
        assertThat(indexNames()).contains(
                "uk_files_object_key", "idx_files_owner_id", "idx_files_status_created_at");
    }

    @Test
    void doesNotCascadeFileRowsWhenOwnerIsDeleted() {
        adapter.save(pendingPhoto(OWNER_ID, "keeps-object-key.jpg"));

        // CASCADE 였다면 사용자 삭제가 조용히 성공하고 object_key 가 사라져 S3 객체를 회수할 수 없다.
        assertThatThrownBy(() -> jdbcTemplate.update(
                "DELETE FROM users WHERE user_id = ?", OWNER_ID))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ------------------------------------------------------------------ 헬퍼

    private static StoredFile pendingPhoto(UUID ownerId, String objectKey) {
        return StoredFile.pending(
                ownerId, FilePurpose.TREATMENT_PHOTO,
                "TREATMENT_PHOTO/" + ownerId + "/" + objectKey, "image/jpeg", 1_024);
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

    private List<String> indexNames() {
        return jdbcTemplate.queryForList(
                "SELECT indexname FROM pg_indexes WHERE tablename = 'files'", String.class);
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
