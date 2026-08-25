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

    /**
     * READY 고아 판정 분기다. 네이티브 SQL 은 바인드 파라미터로 테이블 이름을 받을 수 없어,
     * "파일을 참조하는 테이블" 목록이 이 상수 안에 하드코딩된다. 현재 READY 파일을 참조하는
     * 도메인은 시술기록 사진({@code treatment_record_photos}) 하나뿐이다. 새 도메인이 READY
     * 파일을 참조하게 되면 여기에 조건을 덧대지 않으면 그 도메인이 붙인 파일까지 고아로
     * 판정돼 사진이 통째로 정리된다 — 파일 참조 기능을 추가할 때는 반드시 이 상수를 함께 고친다.
     */
    String READY_ORPHAN_CLAUSE = """
               OR (
                   f.status = 'READY'
                   AND f.created_at <= :readyCreatedBefore
                   AND NOT EXISTS (
                       SELECT 1 FROM treatment_record_photos photo
                       WHERE photo.file_id = f.file_id
                   )
               )""";

    /**
     * 만료된 PENDING·DELETED 세션과 아무도 참조하지 않는 오래된 READY 파일(고아)을 정리
     * 대상으로 꺼낸다. DELETED 는 회수 표시({@code reclaimed_at})와 무관하게 후보에 남는다 —
     * 표시가 찼다는 건 객체만 회수됐다는 뜻이고, 행은 정리 경로가 메타데이터만 지워 마무리한다.
     */
    @Query(value = """
            SELECT f.* FROM files f
            WHERE (f.status = 'DELETED' AND f.expires_at <= :pendingExpiredBefore)
               OR (f.status = 'PENDING' AND f.expires_at <= :pendingExpiredBefore)
            """ + READY_ORPHAN_CLAUSE + """

            ORDER BY f.updated_at, f.file_id
            """, nativeQuery = true)
    List<FileEntity> findCleanupCandidates(
            @Param("pendingExpiredBefore") Instant pendingExpiredBefore,
            @Param("readyCreatedBefore") Instant readyCreatedBefore,
            Pageable pageable);

    @Query(value = "SELECT pg_try_advisory_xact_lock(470047)", nativeQuery = true)
    boolean tryAcquireCleanupLock();
}
