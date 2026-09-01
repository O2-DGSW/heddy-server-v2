package com.heddy.domain.recommendation.port.out;

import com.heddy.domain.recommendation.model.RecommendationRun;

import java.util.Optional;
import java.util.UUID;

public interface RecommendationRepositoryPort {
    RecommendationRun insert(RecommendationRun run, String canonicalInputSnapshot);

    Optional<RecommendationRun> findActiveByInputHash(UUID userId, String strategy, String inputHash);

    Optional<RecommendationRun> findLatestByUserId(UUID userId);

    Optional<RecommendationRun> findByIdAndUserId(UUID recommendationRunId, UUID userId);
}
