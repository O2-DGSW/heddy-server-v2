package com.heddy.domain.analysis.model;

import java.util.Map;
import java.util.Objects;

/** 로컬 모델이 사진 한 장에서 산출한 성공 결과. DB 식별자는 애플리케이션 계층이 붙인다. */
public record HairAnalysisPrediction(
        Map<MetricType, MetricScore> metrics,
        MetricScore confidence,
        String modelVersion,
        String summary,
        String evidence
) {
    public HairAnalysisPrediction {
        metrics = metrics == null ? Map.of() : Map.copyOf(metrics);
        for (MetricType type : MetricType.values()) {
            Objects.requireNonNull(metrics.get(type), "빠진 분석 지표: " + type);
        }
        Objects.requireNonNull(confidence, "confidence");
        if (modelVersion == null || modelVersion.isBlank()) {
            throw new IllegalArgumentException("modelVersion 은 필수입니다");
        }
    }
}
