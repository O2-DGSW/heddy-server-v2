package com.heddy.adapter.out.persistence.analysis;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
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

    /**
     * 기록별 최신 작업의 상태만 한 번에 뽑는다. DISTINCT ON 은 idx_analysis_jobs_record_created
     * 의 정렬을 그대로 따라가 기록마다 첫 행에서 멈춘다 — 작업 전체를 읽어 애플리케이션에서
     * 추리면 기록당 작업 수만큼 헛읽는다.
     *
     * <p>JPQL 에는 DISTINCT ON 이 없고, 같은 일을 상관 서브쿼리로 쓰면 기록마다 한 번씩 도는
     * 계획이 나온다. 읽기 전용 질의 하나라 네이티브로 둔다.
     */
    @Query(value = """
            SELECT DISTINCT ON (record_id) record_id AS recordId, status AS status
            FROM analysis_jobs
            WHERE record_id IN (:recordIds)
            ORDER BY record_id, created_at DESC, job_id DESC
            """, nativeQuery = true)
    List<LatestJobStatusRow> findLatestStatuses(@Param("recordIds") Collection<UUID> recordIds);

    /** 위 질의의 한 행. 목록 배지에 필요한 건 기록과 상태 둘뿐이다. */
    interface LatestJobStatusRow {

        UUID getRecordId();

        String getStatus();
    }
}
