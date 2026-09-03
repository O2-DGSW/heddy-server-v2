package com.heddy.domain.recommendation.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RecommendationRun(
        UUID recommendationRunId,
        UUID userId,
        Strategy strategy,
        Status status,
        String inputHash,
        boolean fallback,
        RecommendationBasis recommendationBasis,
        Instant generatedAt,
        List<RecommendationItem> items
) {
    public enum Strategy { RULE_BASED_V1 }
    public enum Status { ACTIVE, STALE }

    public RecommendationRun {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public RecommendationRun markStale() {
        return new RecommendationRun(recommendationRunId, userId, strategy, Status.STALE,
                inputHash, fallback, recommendationBasis, generatedAt, items);
    }
}
