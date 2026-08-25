package com.heddy.application.file.service;

import com.heddy.adapter.out.persistence.file.FilePersistenceAdapter;
import com.heddy.domain.file.model.FilePurpose;
import com.heddy.domain.file.model.FileStatus;
import com.heddy.domain.file.model.StoredFile;
import com.heddy.domain.file.port.out.FileRepositoryPort;
import com.heddy.domain.file.port.out.FileStoragePort;
import com.heddy.support.PostgresIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 건별 트랜잭션(REQUIRES_NEW) 격리와 회수 표시 존중을 실제 PostgreSQL 상대로 고정해서 본다.
 *
 * <p>테스트 클래스를 {@code @Transactional} 로 감싸지 않는 이유는 후보의 커밋이 그 안쪽
 * 별도 트랜잭션에서 일어나기 때문이다. 감싸면 롤백이 격리 단위를 뚫지 못해 검증이 무너진다.
 * 대신 데이터를 수동으로 치운다.
 */
class FileCleanupIsolationIntegrationTest extends PostgresIntegrationTest {

    private static final UUID USER_ID = UUID.fromString("69000000-0000-4000-8000-000000000001");

    @Autowired FileCleanupScheduler scheduler;
    @Autowired FileRepositoryPort fileRepositoryPort;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired PlatformTransactionManager transactionManager;

    /** 준비 작업용 트랜잭션. 테스트 클래스가 트랜잭션으로 감싸지 않아 따로 둔다. */
    private TransactionTemplate tx;

    /** 스토리지는 호출 기록만 필요하다 — 객체 저장소 없이 정리 경로를 본다. */
    @MockitoBean FileStoragePort fileStoragePort;
    /** 건별 트랜잭션 안에서 DB 수준 실패를 심기 위해 본 어댑터를 엿본다. */
    @MockitoSpyBean FilePersistenceAdapter filePersistenceAdapter;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(transactionManager);
        cleanUpRows();
        jdbcTemplate.update("""
                INSERT INTO users (
                    user_id, email, password_hash, auth_provider, status, login_fail_count
                ) VALUES (?, ?, ?, 'EMAIL', 'ACTIVE', 0)
                """, USER_ID, "cleanup-isolation@example.com", "hash");
    }

    @AfterEach
    void cleanUp() {
        cleanUpRows();
    }

    /**
     * 한 후보의 메타데이터 삭제가 DB 제약 위반으로 터지는 순서다. 공유 트랜잭션이었다면
     * 세션이 깨져 앞뒤 후보까지 연쇄로 놓치고 카운트도 무효가 된다. 건별 트랜잭션은 실패한
     * 후보만 원점(PENDING)으로 돌리고 나머지는 계속 마무리한다.
     */
    @Test
    void commitsEachCandidateIndependentlyWhenOneHitsADatabaseFailureMidRun() {
        StoredFile first = insertExpiredPending("first.jpg", Timestamp.from(
                Instant.now().minus(30, ChronoUnit.MINUTES)));
        StoredFile broken = insertExpiredPending("broken.jpg", Timestamp.from(
                Instant.now().minus(20, ChronoUnit.MINUTES)));
        StoredFile last = insertExpiredPending("last.jpg", Timestamp.from(
                Instant.now().minus(10, ChronoUnit.MINUTES)));
        doAnswer(invocation -> {
            UUID fileId = invocation.getArgument(0);
            if (fileId.equals(broken.fileId())) {
                // 같은 커넥션, 즉 후보 자신의 트랜잭션 안에서 낸 제약 위반이어야 한다.
                jdbcTemplate.update(
                        "UPDATE files SET status = NULL WHERE file_id = ?", fileId);
                return null;
            }
            return invocation.callRealMethod();
        }).when(filePersistenceAdapter).deleteMetadata(any());

        var result = scheduler.cleanup();

        assertThat(result.deleted()).isEqualTo(2);
        assertThat(result.failed()).isEqualTo(1);
        assertThat(fileRepositoryPort.findById(first.fileId())).isEmpty();
        assertThat(fileRepositoryPort.findById(last.fileId())).isEmpty();
        assertThat(fileRepositoryPort.findById(broken.fileId())).isPresent();
        assertThat(statusOf(broken)).isEqualTo(FileStatus.PENDING);
        verify(fileStoragePort).deleteObject(first.objectKey());
        verify(fileStoragePort).deleteObject(last.objectKey());
        verify(fileStoragePort).deleteObject(broken.objectKey());
    }

    /**
     * 회수 표시가 찼다는 건 만료 이후 회수 경로가 객체를 이미 지웠다는 뜻이다. 정리 경로가
     * 같은 키를 또 지우는 중복은 스토리지 호출만 늘린다 — 메타데이터만으로 마무리한다.
     */
    @Test
    void finishesAReclaimedCancelledSessionWithoutTouchingStorageAgain() {
        StoredFile cancelled = insertExpiredPending("reclaimed.jpg",
                Timestamp.from(Instant.now().minus(30, ChronoUnit.MINUTES)));
        tx.executeWithoutResult(status -> {
            fileRepositoryPort.transition(cancelled.markDeleted(), FileStatus.PENDING);
            fileRepositoryPort.markReclaimed(cancelled.fileId(), Instant.now());
        });

        var result = scheduler.cleanup();

        assertThat(result.deleted()).isEqualTo(1);
        assertThat(result.failed()).isZero();
        assertThat(fileRepositoryPort.findById(cancelled.fileId())).isEmpty();
        verify(fileStoragePort, never()).deleteObject(cancelled.objectKey());
    }

    /** 표시가 비어 있으면 객체가 살아 있을 수 있으니 정리 경로가 직접 지우고 행을 마무리한다. */
    @Test
    void deletesTheObjectOfACancelledSessionNobodyHasReclaimedYet() {
        StoredFile cancelled = insertExpiredPending("unreclaimed.jpg",
                Timestamp.from(Instant.now().minus(30, ChronoUnit.MINUTES)));
        tx.executeWithoutResult(status ->
                fileRepositoryPort.transition(cancelled.markDeleted(), FileStatus.PENDING));

        var result = scheduler.cleanup();

        assertThat(result.deleted()).isEqualTo(1);
        assertThat(fileRepositoryPort.findById(cancelled.fileId())).isEmpty();
        verify(fileStoragePort).deleteObject(cancelled.objectKey());
    }

    // ------------------------------------------------------------------ 헬퍼

    private StoredFile insertExpiredPending(String name, Timestamp updatedAt) {
        StoredFile file = fileRepositoryPort.insert(StoredFile.pending(
                USER_ID, FilePurpose.TREATMENT_PHOTO, "TREATMENT_PHOTO/" + USER_ID + "/" + name,
                "image/jpeg", name, 1_024, "b".repeat(64),
                Instant.now().minus(5, ChronoUnit.MINUTES)));
        // 대상 선정은 updated_at 순이다. 후보 간 처리 순서를 고정하려고 직접 심는다.
        jdbcTemplate.update("UPDATE files SET updated_at = ? WHERE file_id = ?",
                updatedAt, file.fileId());
        return file;
    }

    private FileStatus statusOf(StoredFile file) {
        return fileRepositoryPort.findById(file.fileId()).orElseThrow().status();
    }

    private void cleanUpRows() {
        jdbcTemplate.update("DELETE FROM files WHERE user_id = ?", USER_ID);
        jdbcTemplate.update("DELETE FROM users WHERE user_id = ?", USER_ID);
    }
}
