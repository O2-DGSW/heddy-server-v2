package com.heddy.domain.analysis.model;

import com.heddy.domain.analysis.exception.AnalysisError;
import com.heddy.domain.analysis.exception.AnalysisException;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 작업 한 건이 낸 분석 결과. 지표 4종과 신뢰도, 그리고 그 점수를 낸 모델을 함께 담는다.
 *
 * <p>불변식은 이 모델만 책임진다.
 * <ul>
 *   <li>지표 4종이 모두 있어야 한다 — 일부만 담긴 결과는 비교 분석이 읽을 수 없다</li>
 *   <li>{@code modelVersion} 은 필수 — 어느 모델이 낸 점수인지 모르면 과거 결과와 비교할 수 없다</li>
 *   <li>점수 범위와 등급은 {@link MetricScore} 가 본다</li>
 * </ul>
 *
 * <p>등급을 점수에서 계산하지 않는 이유는 {@link ConfidenceGrade} 에 적었다.
 *
 * <p>소유자를 작업이나 기록에서 조인하지 않고 직접 들고 있다. 남의 결과는 없는 결과와 같은
 * 404 여야 하고 질의 횟수도 같아야 한다(#31 컨벤션).
 */
public record AnalysisResult(
        UUID analysisId,
        UUID jobId,
        UUID userId,
        UUID recordId,
        UUID photoId,
        Map<MetricType, MetricScore> metrics,
        MetricScore confidence,
        String modelVersion,
        String summary,
        String evidence,
        Instant analyzedAt
) {
    public AnalysisResult {
        Objects.requireNonNull(analysisId, "analysisId");
        Objects.requireNonNull(jobId, "jobId");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(recordId, "recordId");
        Objects.requireNonNull(confidence, "confidence");
        Objects.requireNonNull(analyzedAt, "analyzedAt");

        // Map.copyOf 는 null 키·값에서 이미 NPE 를 내므로 빠진 지표만 본다.
        metrics = metrics == null ? Map.of() : Map.copyOf(metrics);
        for (MetricType type : MetricType.values()) {
            if (!metrics.containsKey(type)) {
                throw new AnalysisException(AnalysisError.RESULT_METRICS_INCOMPLETE);
            }
        }
        if (modelVersion == null || modelVersion.isBlank()) {
            throw new AnalysisException(AnalysisError.RESULT_MODEL_VERSION_REQUIRED);
        }
    }

    /** AI 서버 콜백이 도착했을 때 결과를 만든다. */
    public static AnalysisResult create(
            AnalysisJob job,
            Map<MetricType, MetricScore> metrics,
            MetricScore confidence,
            String modelVersion,
            String summary,
            String evidence,
            Instant analyzedAt
    ) {
        return new AnalysisResult(UUID.randomUUID(), job.jobId(), job.userId(), job.recordId(),
                job.photoId(), metrics, confidence, modelVersion, summary, evidence, analyzedAt);
    }

    /** 이미 읽어 온 행을 도메인으로 되돌릴 때 쓴다. 불변식을 다시 통과한다. */
    public static AnalysisResult reconstitute(
            UUID analysisId, UUID jobId, UUID userId, UUID recordId, UUID photoId,
            Map<MetricType, MetricScore> metrics, MetricScore confidence, String modelVersion,
            String summary, String evidence, Instant analyzedAt
    ) {
        return new AnalysisResult(analysisId, jobId, userId, recordId, photoId, metrics,
                confidence, modelVersion, summary, evidence, analyzedAt);
    }

    public MetricScore metric(MetricType type) {
        return metrics.get(type);
    }

    /**
     * 비교 분석에서 견줄 수 있는 지표만 고른다. 신뢰도가 낮은 지표는 두 결과의 차를 내도
     * 의미가 없어 {@code comparable=false} 로 나간다(스펙 AI 분석 절).
     */
    public Map<MetricType, MetricScore> comparableMetrics() {
        Map<MetricType, MetricScore> comparable = new EnumMap<>(MetricType.class);
        metrics.forEach((type, metric) -> {
            if (metric.grade().isComparable()) {
                comparable.put(type, metric);
            }
        });
        return Map.copyOf(comparable);
    }
}
