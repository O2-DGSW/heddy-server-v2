package com.heddy.domain.recommendation.port.in;

import java.util.UUID;

public interface GetRecommendationUseCase {
    RecommendationResult get(UUID userId, UUID recommendationRunId);
}
