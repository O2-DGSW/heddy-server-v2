package com.heddy.application.file.service;

import com.heddy.domain.file.exception.FileError;
import com.heddy.domain.file.exception.FileException;
import com.heddy.domain.file.model.FilePurpose;
import com.heddy.domain.file.model.FileStatus;
import com.heddy.domain.file.model.PresignedUpload;
import com.heddy.domain.file.model.StorageObject;
import com.heddy.domain.file.model.StoredFile;
import com.heddy.domain.file.port.in.CancelUploadCommand;
import com.heddy.domain.file.port.in.CompleteUploadCommand;
import com.heddy.domain.file.port.out.FileRepositoryPort;
import com.heddy.domain.file.port.out.FileStoragePort;
import com.heddy.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * complete 와 cancel 이 같은 PENDING 세션을 두고 겹치는 순서를 실제 DB 를 상대로 고정해서 본다.
 *
 * <p>스레드를 띄우지 않는 이유는 어느 쪽이 이길지에 따라 결과가 갈려 테스트가 간헐적으로만
 * 통과하기 때문이다. 문제가 되는 인터리빙은 "둘 다 PENDING 을 읽은 뒤 한쪽이 먼저 전이한다"는
 * 한 가지이므로, 읽기와 전이 사이에 상대의 전이를 끼워 넣어 그 순서를 직접 만든다.
 */
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UploadCancellationRaceIntegrationTest extends PostgresIntegrationTest {

    private static final UUID USER_ID = UUID.fromString("70000000-0000-4000-8000-000000000001");
    private static final long SESSION_TTL_SECONDS = 300;
    private static final StorageObject UPLOADED = new StorageObject("image/jpeg", 1_024);

    @Autowired FileRepositoryPort fileRepositoryPort;
    @Autowired JdbcTemplate jdbcTemplate;

    private final RecordingStorage storage = new RecordingStorage();

    @BeforeEach
    void setUpOwner() {
        jdbcTemplate.update("""
                INSERT INTO users (
                    user_id, email, password_hash, auth_provider, status, login_fail_count
                ) VALUES (?, ?, ?, 'EMAIL', 'ACTIVE', 0)
                """, USER_ID, "race-owner@example.com", "hash");
    }

    /**
     * complete 가 먼저 READY 로 이긴 순서. cancel 은 행 선점에 실패하므로 스토리지를 건드리지
     * 않는다 — 객체를 먼저 지웠다면 트랜잭션 롤백이 그 삭제를 되돌리지 못해 "READY 행 + 없는
     * 객체"가 남는다.
     */
    @Test
    void leavesStorageUntouchedWhenCompleteWinsTheRowBeforeCancellationClaimsIt() {
        StoredFile pending = insertPending("complete-wins.jpg");
        UploadSessionService service = serviceReadingThenLetting(session ->
                fileRepositoryPort.transition(session.markReady(UPLOADED), FileStatus.PENDING));

        assertThatThrownBy(() -> service.cancel(new CancelUploadCommand(USER_ID, pending.uploadId())))
                .isInstanceOfSatisfying(FileException.class, exception ->
                        assertThat(exception.error()).isEqualTo(FileError.CONCURRENT_MODIFICATION));

        assertThat(storage.deletedKeys).isEmpty();
        assertThat(statusOf(pending)).isEqualTo(FileStatus.READY);
    }

    /**
     * 반대 순서 — cancel 이 먼저 행을 선점하고 객체를 지운 뒤 complete 의 전이가 도착한다.
     * 늦게 온 complete 는 0 행으로 거부돼야 한다. 통과시키면 객체가 없는 READY 행이 남는다.
     */
    @Test
    void refusesLateCompletionAfterCancellationClaimedTheRow() {
        StoredFile pending = insertPending("cancel-wins.jpg");
        UploadSessionService service = serviceReadingThenLetting(session ->
                new UploadSessionService(fileRepositoryPort, storage, SESSION_TTL_SECONDS)
                        .cancel(new CancelUploadCommand(USER_ID, session.uploadId())));

        assertThatThrownBy(() -> service.complete(
                new CompleteUploadCommand(USER_ID, pending.uploadId())))
                .isInstanceOfSatisfying(FileException.class, exception ->
                        assertThat(exception.error()).isEqualTo(FileError.CONCURRENT_MODIFICATION));

        assertThat(storage.deletedKeys).containsExactly(pending.objectKey());
        assertThat(statusOf(pending)).isEqualTo(FileStatus.DELETED);
        // 회수 표시가 비어 있어야 만료 이후 회수 경로가 되살아난 객체를 다시 지울 수 있다.
        assertThat(fileRepositoryPort.findReclaimTargets(
                Instant.now().plus(1, ChronoUnit.HOURS), 10))
                .extracting(StoredFile::fileId)
                .contains(pending.fileId());
    }

    // ------------------------------------------------------------------ 헬퍼

    /** 읽기 직후 상대의 전이를 한 번 끼워 넣는 서비스. */
    private UploadSessionService serviceReadingThenLetting(Consumer<StoredFile> opponent) {
        return new UploadSessionService(
                new InterleavingRepository(fileRepositoryPort, opponent), storage,
                SESSION_TTL_SECONDS);
    }

    private StoredFile insertPending(String name) {
        return fileRepositoryPort.insert(StoredFile.pending(
                USER_ID, FilePurpose.TREATMENT_PHOTO, "TREATMENT_PHOTO/" + USER_ID + "/" + name,
                "image/jpeg", name, UPLOADED.byteSize(), "b".repeat(64),
                Instant.now().plus(5, ChronoUnit.MINUTES)));
    }

    private FileStatus statusOf(StoredFile file) {
        return fileRepositoryPort.findById(file.fileId()).orElseThrow().status();
    }

    /** 세션을 읽어 돌려주기 직전에 상대 요청을 한 번만 실행한다. */
    private static final class InterleavingRepository implements FileRepositoryPort {

        private final FileRepositoryPort delegate;
        private final Consumer<StoredFile> opponent;
        private boolean opponentRan;

        private InterleavingRepository(FileRepositoryPort delegate, Consumer<StoredFile> opponent) {
            this.delegate = delegate;
            this.opponent = opponent;
        }

        @Override
        public Optional<StoredFile> findByUploadId(UUID uploadId) {
            Optional<StoredFile> found = delegate.findByUploadId(uploadId);
            if (!opponentRan) {
                opponentRan = true;
                found.ifPresent(opponent);
            }
            return found;
        }

        @Override
        public StoredFile insert(StoredFile file) {
            return delegate.insert(file);
        }

        @Override
        public StoredFile transition(StoredFile file, FileStatus expectedStatus) {
            return delegate.transition(file, expectedStatus);
        }

        @Override
        public Optional<StoredFile> findById(UUID fileId) {
            return delegate.findById(fileId);
        }

        @Override
        public List<StoredFile> findReclaimTargets(Instant now, int limit) {
            return delegate.findReclaimTargets(now, limit);
        }

        @Override
        public void markReclaimed(UUID fileId, Instant reclaimedAt) {
            delegate.markReclaimed(fileId, reclaimedAt);
        }

        @Override
        public List<StoredFile> findAllByUserId(UUID userId) {
            return delegate.findAllByUserId(userId);
        }

        @Override
        public List<StoredFile> findCleanupCandidates(
                Instant pendingExpiredBefore, Instant readyCreatedBefore, int limit
        ) {
            return delegate.findCleanupCandidates(pendingExpiredBefore, readyCreatedBefore, limit);
        }

        @Override
        public boolean tryAcquireCleanupLock() {
            return delegate.tryAcquireCleanupLock();
        }

        @Override
        public void deleteMetadata(UUID fileId) {
            delegate.deleteMetadata(fileId);
        }
    }

    /** 삭제 호출만 기록한다. 이 테스트가 보는 것은 "스토리지를 건드렸는가"다. */
    private static final class RecordingStorage implements FileStoragePort {

        private final List<String> deletedKeys = new ArrayList<>();

        @Override
        public PresignedUpload createUploadUrl(StoredFile file) {
            throw new UnsupportedOperationException();
        }

        @Override
        public URI createDownloadUrl(StoredFile file) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<StorageObject> findObject(String objectKey) {
            return Optional.of(UPLOADED);
        }

        @Override
        public void deleteObject(String objectKey) {
            deletedKeys.add(objectKey);
        }
    }
}
