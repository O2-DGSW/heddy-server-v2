package com.heddy.domain.recommendation.port.in;

import com.heddy.domain.recommendation.model.HairstyleCandidate;
import com.heddy.domain.recommendation.model.RecommendationItem;
import com.heddy.domain.recommendation.model.RecommendationBasis;
import com.heddy.domain.recommendation.model.RecommendationRun;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RecommendationResult(
        UUID recommendationRunId,
        RecommendationRun.Strategy strategy,
        RecommendationRun.Status status,
        Instant generatedAt,
        boolean fallback,
        RecommendationBasis recommendationBasis,
        List<Item> items
) {
    public RecommendationResult {
        items = List.copyOf(items);
    }

    public record Item(
            RecommendationItem recommendation,
            HairstyleCandidate hairstyle,
            URI thumbnailUrl
    ) { }
}
