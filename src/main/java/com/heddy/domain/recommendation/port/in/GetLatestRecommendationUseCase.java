package com.heddy.domain.recommendation.port.in;

import java.util.UUID;

public interface GetLatestRecommendationUseCase {
    RecommendationResult getLatest(UUID userId);
}
