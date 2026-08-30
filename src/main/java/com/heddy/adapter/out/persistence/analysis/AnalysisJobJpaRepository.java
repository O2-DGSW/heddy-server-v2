package com.heddy.adapter.out.persistence.analysis;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface AnalysisJobJpaRepository extends JpaRepository<AnalysisJobEntity, UUID> {

    Optional<AnalysisJobEntity> findByJobIdAndUserId(UUID jobId, UUID userId);

    /**
     * 기록의 최신 작업. 접수 순서가 곧 최신 순서이고, 같은 시각의 행이 갈리도록 job_id 를
     * 정렬 키에 함께 둔다(인덱스 정렬과 같은 순서다).
     */
    @Query("""
            SELECT job FROM AnalysisJobEntity job
            WHERE job.recordId = :recordId
            ORDER BY job.createdAt DESC, job.jobId DESC
            """)
    List<AnalysisJobEntity> findLatestByRecordId(@Param("recordId") UUID recordId, Limit limit);

    /** 진행 중 작업은 부분 UNIQUE 인덱스가 사진당 하나로 묶는다. */
    Optional<AnalysisJobEntity> findByPhotoIdAndStatusIn(UUID photoId, List<String> statuses);
}
