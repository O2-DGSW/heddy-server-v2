package com.heddy.adapter.out.persistence.analysis;

import com.heddy.adapter.out.persistence.BaseEntity;
import com.heddy.domain.analysis.exception.AnalysisError;
import com.heddy.domain.analysis.exception.AnalysisException;
import com.heddy.domain.analysis.model.AnalysisResult;
import com.heddy.domain.analysis.model.ConfidenceGrade;
import com.heddy.domain.analysis.model.MetricScore;
import com.heddy.domain.analysis.model.MetricType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * 분석 결과의 JPA 표현. 도메인은 지표를 map 으로 다루고 행은 컬럼으로 편다 — 지표 4종은
 * 스펙이 고정한 집합이라 컬럼이어야 비교 분석 질의가 단순하고, 도메인 쪽은 지표를 하나씩
 * 나열하지 않아야 지표가 늘 때 손댈 곳이 줄어든다.
 */
@Entity
@Table(name = "analysis_results")
class AnalysisResultEntity extends BaseEntity {

    @Id
    @Column(name = "analysis_id", nullable = false, updatable = false)
    private UUID analysisId;

    @Column(name = "job_id", nullable = false, unique = true, updatable = false)
    private UUID jobId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "record_id", nullable = false, updatable = false)
    private UUID recordId;

    /** 사진이 지워지면 DB 가 NULL 로 비운다(SET NULL). 결과 이력은 그대로 남는다. */
    @Column(name = "photo_id", updatable = false)
    private UUID photoId;

    @Column(name = "color_uniformity_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal colorUniformityScore;

    @Column(name = "color_uniformity_grade", nullable = false, length = 10)
    private String colorUniformityGrade;

    @Column(name = "shape_symmetry_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal shapeSymmetryScore;

    @Column(name = "shape_symmetry_grade", nullable = false, length = 10)
    private String shapeSymmetryGrade;

    @Column(name = "volume_balance_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal volumeBalanceScore;

    @Column(name = "volume_balance_grade", nullable = false, length = 10)
    private String volumeBalanceGrade;

    @Column(name = "roughness_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal roughnessScore;

    @Column(name = "roughness_grade", nullable = false, length = 10)
    private String roughnessGrade;

    @Column(name = "confidence_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal confidenceScore;

    @Column(name = "confidence_grade", nullable = false, length = 10)
    private String confidenceGrade;

    @Column(name = "model_version", nullable = false, length = 50)
    private String modelVersion;

    @Column(name = "summary", length = 500)
    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evidence")
    private String evidence;

    @Column(name = "analyzed_at", nullable = false)
    private Instant analyzedAt;

    protected AnalysisResultEntity() {
    }

    AnalysisResultEntity(AnalysisResult result) {
        analysisId = result.analysisId();
        jobId = result.jobId();
        userId = result.userId();
        recordId = result.recordId();
        photoId = result.photoId();
        colorUniformityScore = scoreOf(result, MetricType.COLOR_UNIFORMITY);
        colorUniformityGrade = gradeOf(result, MetricType.COLOR_UNIFORMITY);
        shapeSymmetryScore = scoreOf(result, MetricType.SHAPE_SYMMETRY);
        shapeSymmetryGrade = gradeOf(result, MetricType.SHAPE_SYMMETRY);
        volumeBalanceScore = scoreOf(result, MetricType.VOLUME_BALANCE);
        volumeBalanceGrade = gradeOf(result, MetricType.VOLUME_BALANCE);
        roughnessScore = scoreOf(result, MetricType.ROUGHNESS);
        roughnessGrade = gradeOf(result, MetricType.ROUGHNESS);
        confidenceScore = result.confidence().score();
        confidenceGrade = result.confidence().grade().name();
        modelVersion = result.modelVersion();
        summary = result.summary();
        evidence = result.evidence();
        analyzedAt = result.analyzedAt();
    }

    AnalysisResult toDomain() {
        Map<MetricType, MetricScore> metrics = new EnumMap<>(MetricType.class);
        metrics.put(MetricType.COLOR_UNIFORMITY,
                metric(colorUniformityScore, colorUniformityGrade));
        metrics.put(MetricType.SHAPE_SYMMETRY, metric(shapeSymmetryScore, shapeSymmetryGrade));
        metrics.put(MetricType.VOLUME_BALANCE, metric(volumeBalanceScore, volumeBalanceGrade));
        metrics.put(MetricType.ROUGHNESS, metric(roughnessScore, roughnessGrade));
        return AnalysisResult.reconstitute(analysisId, jobId, userId, recordId, photoId, metrics,
                metric(confidenceScore, confidenceGrade), modelVersion, summary, evidence,
                analyzedAt);
    }

    private static BigDecimal scoreOf(AnalysisResult result, MetricType type) {
        return result.metric(type).score();
    }

    private static String gradeOf(AnalysisResult result, MetricType type) {
        return result.metric(type).grade().name();
    }

    private static MetricScore metric(BigDecimal score, String grade) {
        return new MetricScore(score, parseGrade(grade));
    }

    private static ConfidenceGrade parseGrade(String grade) {
        try {
            return ConfidenceGrade.valueOf(grade);
        } catch (IllegalArgumentException invalidName) {
            throw new AnalysisException(AnalysisError.RESULT_GRADE_UNKNOWN);
        }
    }
}
