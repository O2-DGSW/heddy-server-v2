package com.heddy.adapter.out.persistence.file;

import com.heddy.domain.file.model.FileStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
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
}
