package com.heddy.adapter.out.persistence.file;

import com.heddy.domain.file.model.FileStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface FileJpaRepository extends JpaRepository<FileEntity, UUID> {

    /**
     * 기대 상태를 WHERE 에 넣은 조건부 갱신. 그 사이 다른 요청이 상태를 바꿔놨으면 0 행이 나온다.
     *
     * <p>영속성 컨텍스트를 거치지 않는 벌크 갱신이라 {@code @LastModifiedDate} 감사가 돌지 않는다.
     * {@code updatedAt} 을 직접 넣는 이유다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE FileEntity f
               SET f.status = :status,
                   f.contentType = :contentType,
                   f.fileSize = :fileSize,
                   f.sha256 = :sha256,
                   f.width = :width,
                   f.height = :height,
                   f.updatedAt = :now
             WHERE f.fileId = :fileId
               AND f.status = :expectedStatus
            """)
    int applyTransition(
            @Param("fileId") UUID fileId,
            @Param("expectedStatus") FileStatus expectedStatus,
            @Param("status") FileStatus status,
            @Param("contentType") String contentType,
            @Param("fileSize") long fileSize,
            @Param("sha256") String sha256,
            @Param("width") Integer width,
            @Param("height") Integer height,
            @Param("now") Instant now);

    Optional<FileEntity> findByUploadId(UUID uploadId);
    /**
     * 만료 이후 다시 회수해야 하는 취소 세션. 취소 시점의 객체 삭제는 presigned PUT URL 이
     * 살아 있는 동안 되살아날 수 있어 최종 회수가 아니다. {@code expiresAt} 이 지나야 URL 로
     * 객체가 다시 생길 수 없으므로 그때 한 번 더 지우고 {@code reclaimedAt} 을 채운다.
     */
    @Query("""
            SELECT f FROM FileEntity f
             WHERE f.status = :status
               AND f.reclaimedAt IS NULL
               AND f.expiresAt <= :now
             ORDER BY f.expiresAt
            """)
    List<FileEntity> findReclaimTargets(
            @Param("status") FileStatus status,
            @Param("now") Instant now,
            Pageable pageable);

    /**
     * 회수 완료 표시. {@code reclaimedAt IS NULL} 을 조건에 넣어 두 정리 작업이 같은 행을 집어도
     * 한 번만 표시되게 한다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE FileEntity f
               SET f.reclaimedAt = :now,
                   f.updatedAt = :now
             WHERE f.fileId = :fileId
               AND f.reclaimedAt IS NULL
            """)
    int markReclaimed(@Param("fileId") UUID fileId, @Param("now") Instant now);

    List<FileEntity> findAllByUserId(UUID userId);

    @Query(value = """
            SELECT f.* FROM files f
            WHERE (f.status = 'DELETED' AND f.expires_at <= :pendingExpiredBefore)
               OR (f.status = 'PENDING' AND f.expires_at <= :pendingExpiredBefore)
               OR (f.status = 'READY' AND f.created_at <= :readyCreatedBefore
                   AND NOT EXISTS (
                       SELECT 1 FROM treatment_record_photos photo
                       WHERE photo.file_id = f.file_id
                   ))
            ORDER BY f.updated_at, f.file_id
            """, nativeQuery = true)
    List<FileEntity> findCleanupCandidates(
            @Param("pendingExpiredBefore") Instant pendingExpiredBefore,
            @Param("readyCreatedBefore") Instant readyCreatedBefore,
            Pageable pageable);

    @Query(value = "SELECT pg_try_advisory_xact_lock(470047)", nativeQuery = true)
    boolean tryAcquireCleanupLock();
}
