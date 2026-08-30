package com.heddy.adapter.out.persistence.analysis;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface AnalysisResultJpaRepository extends JpaRepository<AnalysisResultEntity, UUID> {

    Optional<AnalysisResultEntity> findByAnalysisIdAndUserId(UUID analysisId, UUID userId);

    Optional<AnalysisResultEntity> findByJobId(UUID jobId);

    /** 기록의 최신 결과. 같은 시각의 행이 갈리도록 analysis_id 를 정렬 키에 함께 둔다. */
    @Query("""
            SELECT result FROM AnalysisResultEntity result
            WHERE result.recordId = :recordId
            ORDER BY result.analyzedAt DESC, result.analysisId DESC
            """)
    List<AnalysisResultEntity> findLatestByRecordId(@Param("recordId") UUID recordId, Limit limit);
}
